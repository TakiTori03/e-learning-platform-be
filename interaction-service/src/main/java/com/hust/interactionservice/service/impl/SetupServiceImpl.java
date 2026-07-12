package com.hust.interactionservice.service.impl;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hust.commonlibrary.dto.UserSharedProfile;
import com.hust.commonlibrary.service.RedisService;
import com.hust.interactionservice.client.CourseClient;
import com.hust.interactionservice.entity.BlogPost;
import com.hust.interactionservice.entity.BlogTopic;
import com.hust.interactionservice.entity.enums.BlogStatus;
import com.hust.interactionservice.repository.BlogPostRepository;
import com.hust.interactionservice.repository.BlogTopicRepository;
import com.hust.interactionservice.service.SetupService;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SetupServiceImpl implements SetupService {

    private final BlogTopicRepository topicRepository;
    private final BlogPostRepository blogRepository;
    private final RedisService redisService;
    private final CourseClient courseClient;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    @Transactional
    public void seedBlogs() {
        log.info("Starting mock blog seeding process...");
        try {
            // 1. Seed Topics if empty
            InputStream topicStream = new ClassPathResource("data/blog-topics.json").getInputStream();
            List<BlogTopic> topicsFromJson = objectMapper.readValue(topicStream, new TypeReference<List<BlogTopic>>() {});

            for (BlogTopic topic : topicsFromJson) {
                if (!topicRepository.existsByName(topic.getName())) {
                    topicRepository.save(topic);
                    log.info("Saved Blog Topic: {}", topic.getName());
                }
            }

            // Map Topic slug -> Saved instance
            Map<String, BlogTopic> topicMap = topicRepository.findAll().stream()
                    .collect(Collectors.toMap(BlogTopic::getSlug, t -> t));

            // Get all instructor IDs from Redis to randomly assign as authors
            List<String> instructorIds = new ArrayList<>();
            try {
                Set<String> profileKeys = redisService.keys("elearning:shared:user:profile:*");
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
                log.error("Failed to fetch instructor IDs from Redis for blog seeding: {}", e.getMessage());
            }

            // Fallback default author ID if no instructors found in Redis
            if (instructorIds.isEmpty()) {
                instructorIds.add("default_instructor_id");
            }

            // Fetch course list from course-service Feign client to map courseCodes to actual database IDs
            Map<String, String> courseCodeToIdMap = new HashMap<>();
            try {
                var coursesRes = courseClient.getAllActiveCourses();
                if (coursesRes != null && coursesRes.isSuccess() && coursesRes.getPayload() != null) {
                    for (var course : coursesRes.getPayload()) {
                        String id = course.getId();
                        String code = course.getCode();
                        if (id != null && code != null) {
                            courseCodeToIdMap.put(code, id);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Failed to fetch course details from course-service Feign: {}", e.getMessage());
            }

            // 2. Read mock blogs from JSON file
            InputStream blogStream = new ClassPathResource("data/mock-blogs.json").getInputStream();
            List<MockBlogDto> mockBlogs = objectMapper.readValue(blogStream, new TypeReference<List<MockBlogDto>>() {});

            for (MockBlogDto mockBlog : mockBlogs) {
                if (blogRepository.existsBySlug(mockBlog.getSlug())) {
                    log.info("Blog with slug {} already exists. Skipping.", mockBlog.getSlug());
                    continue;
                }

                BlogTopic topic = topicMap.get(mockBlog.getTopicSlug());
                if (topic == null) {
                    log.warn("Topic slug {} not found for blog post {}. Skipping.", mockBlog.getTopicSlug(), mockBlog.getTitle());
                    continue;
                }

                // Randomly select author
                String authorId = instructorIds.get(ThreadLocalRandom.current().nextInt(instructorIds.size()));

                // Map courseCodes to courseIds
                List<String> courseIds = new ArrayList<>();
                if (mockBlog.getCourseCodes() != null) {
                    for (String code : mockBlog.getCourseCodes()) {
                        String matchedCourseId = courseCodeToIdMap.get(code);
                        if (matchedCourseId != null) {
                            courseIds.add(matchedCourseId);
                        }
                    }
                }

                BlogPost blogPost = BlogPost.builder()
                        .title(mockBlog.getTitle())
                        .slug(mockBlog.getSlug())
                        .content(mockBlog.getContent())
                        .thumbnail(mockBlog.getThumbnail())
                        .topic(topic)
                        .tags(mockBlog.getTags() != null ? mockBlog.getTags() : new ArrayList<>())
                        .status(BlogStatus.PUBLISHED)
                        .isPinned(mockBlog.getIsPinned() != null ? mockBlog.getIsPinned() : false)
                        .viewsCount(mockBlog.getViewsCount() != null ? mockBlog.getViewsCount() : 0L)
                        .courseIds(courseIds)
                        .likes(new ArrayList<>())
                        .reports(new ArrayList<>())
                        .sharesCount(0L)
                        .commentsCount(0L)
                        .build();

                // Setup audits manually for seeding
                blogPost.setCreatedBy(authorId);
                blogPost.setUpdatedBy(authorId);
                blogPost.setCreatedAt(Instant.now());
                blogPost.setUpdatedAt(Instant.now());

                blogRepository.save(blogPost);
                log.info("Seeded Blog Post: {} with associated courses: {}", blogPost.getTitle(), courseIds);
            }

            log.info("Mock blogs seeding process completed successfully.");
        } catch (Exception e) {
            log.error("Failed to seed mock blogs: {}", e.getMessage(), e);
        }
    }

    @Data
    public static class MockBlogDto {
        private String title;
        private String slug;
        private String content;
        private String thumbnail;
        private String topicSlug;
        private List<String> tags;
        private Boolean isPinned;
        private Long viewsCount;
        private List<String> courseCodes;
    }
}
