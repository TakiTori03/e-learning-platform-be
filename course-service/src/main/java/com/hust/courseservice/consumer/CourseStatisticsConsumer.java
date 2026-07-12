package com.hust.courseservice.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.hust.commonlibrary.constants.KafkaTopics;
import com.hust.commonlibrary.event.CourseEnrollmentUpdatedEvent;
import com.hust.commonlibrary.event.CourseReviewUpdatedEvent;
import com.hust.commonlibrary.service.RedisService;
import com.hust.courseservice.repository.CourseRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Kafka Consumer lắng nghe các sự kiện thống kê từ interaction-service và learning-service,
 * cập nhật phi chuẩn hóa (Denormalized) các trường avgRatingStars, numOfReviews, studentCount
 * trực tiếp vào Document Course trên MongoDB để phục vụ tìm kiếm, lọc, sắp xếp hiệu năng cao.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CourseStatisticsConsumer {

    private final CourseRepository courseRepository;
    private final RedisService redisService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    /**
     * Lắng nghe sự kiện cập nhật đánh giá khóa học từ interaction-service.
     * Cập nhật avgRatingStars và numOfReviews vào Course document.
     */
    @KafkaListener(topics = KafkaTopics.COURSE_REVIEW_UPDATED, groupId = "course-service-group")
    public void consumeReviewUpdated(CourseReviewUpdatedEvent event) {
        log.info("📊 Received CourseReviewUpdatedEvent: courseId={}, avgRating={}, numReviews={}",
                event.getCourseId(), event.getAvgRatingStars(), event.getNumOfReviews());

        courseRepository.findById(event.getCourseId()).ifPresentOrElse(course -> {
            course.setAvgRatingStars(event.getAvgRatingStars());
            course.setNumOfReviews(event.getNumOfReviews().intValue());
            courseRepository.save(course);

            publishSearchSyncEvent(course);
            evictCourseDetailCache(event.getCourseId());
            log.info("✅ Updated review stats for Course {}: avgRating={}, numReviews={}",
                    event.getCourseId(), event.getAvgRatingStars(), event.getNumOfReviews());
        }, () -> log.warn("⚠️ Course {} not found, skipping review stats update", event.getCourseId()));
    }

    /**
     * Lắng nghe sự kiện cập nhật số lượng học viên từ learning-service.
     * Cập nhật studentCount vào Course document.
     */
    @KafkaListener(topics = KafkaTopics.COURSE_ENROLLMENT_UPDATED, groupId = "course-service-group")
    public void consumeEnrollmentUpdated(CourseEnrollmentUpdatedEvent event) {
        log.info("📊 Received CourseEnrollmentUpdatedEvent: courseId={}, studentCount={}",
                event.getCourseId(), event.getStudentCount());

        courseRepository.findById(event.getCourseId()).ifPresentOrElse(course -> {
            course.setStudentCount(event.getStudentCount().intValue());
            courseRepository.save(course);

            publishSearchSyncEvent(course);
            evictCourseDetailCache(event.getCourseId());
            log.info("✅ Updated enrollment count for Course {}: studentCount={}",
                    event.getCourseId(), event.getStudentCount());
        }, () -> log.warn("⚠️ Course {} not found, skipping enrollment stats update", event.getCourseId()));
    }

    private void publishSearchSyncEvent(com.hust.courseservice.entity.Course course) {
        try {
            com.hust.commonlibrary.event.CourseSearchSyncEvent event =
                com.hust.commonlibrary.event.CourseSearchSyncEvent.builder()
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
                    .action(com.hust.commonlibrary.enums.SyncAction.UPDATE)
                    .createdAt(course.getCreatedAt())
                    .build();
            eventPublisher.publishEvent(event);
            log.info("Published Spring Application Event for CourseSearchSyncEvent from Statistics Consumer: courseId={}", course.getId());
        } catch (Exception e) {
            log.error("Failed to publish Spring Application Event for CourseSearchSyncEvent from Statistics Consumer: courseId={}, error={}", course.getId(), e.getMessage());
        }
    }

    private void evictCourseDetailCache(String courseId) {
        try {
            String cacheKey = "course:detail:" + courseId;
            redisService.delete(cacheKey);
        } catch (Exception e) {
            log.warn("Non-blocking: Failed to evict cache for course {}: {}", courseId, e.getMessage());
        }
    }
}
