package com.hust.searchservice.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.hust.commonlibrary.constants.KafkaTopics;
import com.hust.commonlibrary.event.CourseSearchSyncEvent;
import com.hust.searchservice.document.CourseDocument;
import com.hust.searchservice.repository.CourseSearchRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Component
@RequiredArgsConstructor
@Slf4j
public class CourseSyncConsumer {

    private final CourseSearchRepository courseSearchRepository;

    @KafkaListener(topics = KafkaTopics.COURSE_SEARCH_SYNC, groupId = "search-service-group")
    public void consumeCourseSync(CourseSearchSyncEvent event) {
        log.info("📥 Received CourseSearchSyncEvent from Kafka: courseId={}, action={}", event.getId(), event.getAction());
        try {
            if (com.hust.commonlibrary.enums.SyncAction.DELETE.equals(event.getAction())) {
                courseSearchRepository.deleteById(event.getId());
                log.info("🗑️ Successfully deleted course {} from Elasticsearch Index.", event.getId());
            } else {
                // CREATE or UPDATE
                CourseDocument doc = CourseDocument.builder()
                        .id(event.getId())
                        .name(event.getName())
                        .subTitle(event.getSubTitle())
                        .description(event.getDescription())
                        .price(event.getPrice())
                        .finalPrice(event.getFinalPrice())
                        .avgRatingStars(event.getAvgRatingStars() != null ? event.getAvgRatingStars() : 0.0)
                        .studentCount(event.getStudentCount() != null ? event.getStudentCount() : 0)
                        .numOfReviews(event.getNumOfReviews() != null ? event.getNumOfReviews() : 0)
                        .level(event.getLevel())
                        .status(event.getStatus())
                        .instructorId(event.getInstructorId())
                        .categoryId(event.getCategoryId())
                        .categoryName(event.getCategoryName())
                        .createdAt(event.getCreatedAt() != null ? event.getCreatedAt() : java.time.Instant.now())
                        .build();

                courseSearchRepository.save(doc);
                log.info("💾 Successfully indexed/updated course {} in Elasticsearch Index.", event.getId());
            }
        } catch (Exception e) {
            log.error("❌ Error processing CourseSearchSyncEvent for courseId {}: ", event.getId(), e);
        }
    }
}
