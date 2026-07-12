package com.hust.courseservice.service.impl;

import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.core.io.ClassPathResource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hust.commonlibrary.constants.KafkaTopics;
import com.hust.commonlibrary.constants.RedisPrefixConstants;
import com.hust.commonlibrary.dto.UserSharedProfile;
import com.hust.commonlibrary.event.CourseSearchSyncEvent;
import com.hust.commonlibrary.service.RedisService;
import com.hust.courseservice.entity.Category;
import com.hust.courseservice.entity.Course;
import com.hust.courseservice.entity.Section;
import com.hust.courseservice.entity.enums.CourseLevel;
import com.hust.courseservice.entity.enums.CourseStatus;
import com.hust.courseservice.repository.CategoryRepository;
import com.hust.courseservice.repository.CourseRepository;
import com.hust.courseservice.repository.SectionRepository;
import com.hust.courseservice.service.SetupService;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SetupServiceImpl implements SetupService {

    private final CategoryRepository categoryRepository;
    private final CourseRepository courseRepository;
    private final SectionRepository sectionRepository;
    private final RedisService redisService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    public void seedCourses() {
        log.info("Starting mock courses seeding process...");
        try {
            // 1. Seed Categories if empty
            InputStream catStream = new ClassPathResource("data/categories.json").getInputStream();
            List<Category> categoriesFromJson = objectMapper.readValue(catStream, new TypeReference<List<Category>>() {});

            for (Category cat : categoriesFromJson) {
                if (!categoryRepository.existsByName(cat.getName())) {
                    categoryRepository.save(cat);
                    log.info("Saved Category: {}", cat.getName());
                }
            }

            // Map Category slug -> Saved instance
            Map<String, Category> categoryMap = categoryRepository.findAll().stream()
                    .collect(Collectors.toMap(Category::getCategorySlug, c -> c));

            // Get all instructor IDs from Redis to randomly assign
            java.util.List<String> instructorIds = new java.util.ArrayList<>();
            try {
                java.util.Set<String> profileKeys = redisService.keys("elearning:shared:user:profile:*");
                if (profileKeys != null) {
                    for (String key : profileKeys) {
                        String actualId = key.substring("elearning:shared:user:profile:".length());
                        UserSharedProfile profile = redisService.get(key, UserSharedProfile.class);
                        if (profile != null && "INSTRUCTOR".equals(profile.getRole())) {
                            instructorIds.add(actualId);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Failed to fetch instructor IDs from Redis: {}", e.getMessage());
            }

            // 2. Read mock courses from JSON file
            InputStream courseStream = new ClassPathResource("data/mock-courses.json").getInputStream();
            List<MockCourseDto> mockCourses = objectMapper.readValue(courseStream, new TypeReference<List<MockCourseDto>>() {});

            for (MockCourseDto mockCourse : mockCourses) {
                Category category = categoryMap.get(mockCourse.getCategorySlug());
                if (category == null) {
                    log.warn("Category slug {} not found. Skipping course {}.", mockCourse.getCategorySlug(), mockCourse.getName());
                    continue;
                }

                String actualInstructorId = null;
                if (!instructorIds.isEmpty()) {
                    int randomIndex = java.util.concurrent.ThreadLocalRandom.current().nextInt(instructorIds.size());
                    actualInstructorId = instructorIds.get(randomIndex);
                }

                Course course;
                boolean isNew = false;
                var existingCourseOpt = courseRepository.findByCode(mockCourse.getCode());
                if (existingCourseOpt.isPresent()) {
                    course = existingCourseOpt.get();
                    course.setInstructorId(actualInstructorId);
                    course.setCategory(category);
                    course.setThumbnail(mockCourse.getThumbnail());
                    course.setCoursePreview(mockCourse.getCoursePreview());
                    course = courseRepository.save(course);
                    log.info("Updated existing Course {} with actual Instructor ID: {}", course.getName(), actualInstructorId);
                } else {
                    isNew = true;
                    // Create Course
                    String slugSuffix = java.util.UUID.randomUUID().toString().substring(0, 6);
                    String slug = toSlug(mockCourse.getName()) + "-" + slugSuffix;

                    course = Course.builder()
                            .code(mockCourse.getCode())
                            .name(mockCourse.getName())
                            .subTitle("Khóa học chất lượng cao chuyên sâu về " + mockCourse.getName())
                            .thumbnail(mockCourse.getThumbnail())
                            .coursePreview(mockCourse.getCoursePreview())
                            .price(mockCourse.getPrice() != null ? mockCourse.getPrice() : 0.0)
                            .finalPrice(mockCourse.getFinalPrice() != null ? mockCourse.getFinalPrice() : 0.0)
                            .description("Đây là mô tả chi tiết mẫu của khóa học " + mockCourse.getName() + ". Khóa học này cung cấp các bài giảng đầy đủ từ cơ bản đến thực hành chuyên sâu nhằm giúp người học làm chủ các kỹ năng cần thiết.")
                            .level(CourseLevel.valueOf(mockCourse.getLevel()))
                            .status(CourseStatus.PUBLISHED)
                            .courseSlug(slug)
                            .instructorId(actualInstructorId)
                            .category(category)
                            .requirements(mockCourse.getRequirements() != null ? mockCourse.getRequirements() : new ArrayList<>())
                            .willLearns(mockCourse.getWillLearns() != null ? mockCourse.getWillLearns() : new ArrayList<>())
                            .tags(List.of(mockCourse.getCategorySlug(), "tutorial", "hust"))
                            .views(150)
                            .studentCount(25)
                            .avgRatingStars(4.8)
                            .numOfReviews(10)
                            .build();

                    course = courseRepository.save(course);
                    log.info("Saved new Course: {} with ID: {}", course.getName(), course.getId());
                }

                // Permanently map courseId -> instructorId on Redis Cluster
                updateCourseOwnerCache(course);

                // Create Sections only if course is new
                if (isNew && mockCourse.getSections() != null) {
                    for (int i = 0; i < mockCourse.getSections().size(); i++) {
                        MockSectionDto sectionDto = mockCourse.getSections().get(i);
                        Section section = Section.builder()
                                .courseId(course.getId())
                                .name(sectionDto.getName())
                                .description(sectionDto.getDescription())
                                .position(i + 1)
                                .build();
                        section = sectionRepository.save(section);
                        log.info("  Saved Section: {} (position {})", section.getName(), section.getPosition());
                    }
                }


                // Sync to Elasticsearch via Kafka directly
                publishSearchSyncEvent(course);
            }
            log.info("Mock courses seeding process completed successfully!");
        } catch (Exception e) {
            log.error("Error occurred during mock courses seeding: {}", e.getMessage(), e);
            throw new RuntimeException("Seeding failed: " + e.getMessage(), e);
        }
    }

    private void updateCourseOwnerCache(Course course) {
        try {
            String key = RedisPrefixConstants.getSharedCourseOwnerKey(course.getId());
            redisService.set(key, course.getInstructorId());
            log.info("Synced Redis: Course {} permanently mapped to Owner {}", course.getId(), course.getInstructorId());
        } catch (Exception e) {
            log.error("Cache Failure: Syncing Redis for Course owner failed: ", e.getMessage());
        }
    }

    private void publishSearchSyncEvent(Course course) {
        try {
            CourseSearchSyncEvent event = CourseSearchSyncEvent.builder()
                    .id(course.getId())
                    .name(course.getName())
                    .subTitle(course.getSubTitle())
                    .description(course.getDescription())
                    .price(course.getPrice())
                    .finalPrice(course.getFinalPrice())
                    .avgRatingStars(course.getAvgRatingStars())
                    .studentCount(course.getStudentCount())
                    .numOfReviews(course.getNumOfReviews())
                    .level(course.getLevel() != null ? course.getLevel().name() : null)
                    .status(course.getStatus() != null ? com.hust.commonlibrary.enums.CourseSyncStatus.valueOf(course.getStatus().name()) : null)
                    .instructorId(course.getInstructorId())
                    .categoryId(course.getCategory() != null ? course.getCategory().getId() : null)
                    .categoryName(course.getCategory() != null ? course.getCategory().getName() : null)
                    .action(com.hust.commonlibrary.enums.SyncAction.CREATE)
                    .createdAt(course.getCreatedAt())
                    .build();

            // Direct Kafka publish to bypass TransactionalEventListener issues on dev setups
            kafkaTemplate.send(KafkaTopics.COURSE_SEARCH_SYNC, course.getId(), event);
            log.info("Directly sent CourseSearchSyncEvent to Kafka topic '{}': courseId={}", KafkaTopics.COURSE_SEARCH_SYNC, course.getId());
        } catch (Exception e) {
            log.error("Failed to publish CourseSearchSyncEvent to Kafka: courseId={}, error={}", course.getId(), e.getMessage());
        }
    }

    private String toSlug(String input) {
        if (input == null || input.isBlank()) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String stripped = Pattern.compile("\\p{M}").matcher(normalized).replaceAll("");
        stripped = stripped.replace('đ', 'd').replace('Đ', 'D');
        String nowhitespace = Pattern.compile("\\s+").matcher(stripped).replaceAll("-");
        String slug = Pattern.compile("[^\\w-]").matcher(nowhitespace).replaceAll("");
        slug = Pattern.compile("-{2,}").matcher(slug).replaceAll("-");
        slug = slug.replaceAll("^-|-$", "");
        return slug.toLowerCase(Locale.ENGLISH);
    }

    @Data
    private static class MockCourseDto {
        private String code;
        private String name;
        private String categorySlug;
        private String level;
        private Double price;
        private Double finalPrice;
        private String thumbnail;
        private String coursePreview;
        private List<String> requirements;
        private List<String> willLearns;
        private List<MockSectionDto> sections;
    }

    @Data
    private static class MockSectionDto {
        private String name;
        private String description;
    }
}
