package com.hust.interactionservice.listener;

import com.hust.commonlibrary.constants.KafkaTopics;
import com.hust.commonlibrary.event.CourseReviewUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewEventListener {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleCourseReviewUpdatedEvent(CourseReviewUpdatedEvent event) {
        log.info("💡 Thread [{}] is picking up local CourseReviewUpdatedEvent for course: {}",
                Thread.currentThread().getName(), event.getCourseId());

        try {
            log.info("🚀 Broadcasting event to Kafka topic '{}' for course: {}",
                    KafkaTopics.COURSE_REVIEW_UPDATED, event.getCourseId());

            kafkaTemplate.send(KafkaTopics.COURSE_REVIEW_UPDATED, event.getCourseId(), event);
            log.info("📤 Successfully sent CourseReviewUpdatedEvent to Kafka for course: {}", event.getCourseId());

        } catch (Exception e) {
            log.error("❌ CRITICAL ERROR publishing to Kafka! Event delivery FAILED. Course: {}. Error: {}",
                    event.getCourseId(), e.getMessage(), e);
        }
    }
}
