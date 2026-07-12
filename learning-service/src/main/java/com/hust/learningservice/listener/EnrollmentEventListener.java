package com.hust.learningservice.listener;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.hust.commonlibrary.constants.KafkaTopics;
import com.hust.commonlibrary.event.CourseEnrollmentUpdatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class EnrollmentEventListener {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCourseEnrollmentUpdatedEvent(CourseEnrollmentUpdatedEvent event) {
        log.info("💡 Transaction COMMITTED successfully! Thread [{}] is picking up local CourseEnrollmentUpdatedEvent: courseId={}, studentCount={}",
                Thread.currentThread().getName(), event.getCourseId(), event.getStudentCount());

        try {
            log.info("🚀 Broadcasting event to Kafka topic '{}' for course: {}",
                    KafkaTopics.COURSE_ENROLLMENT_UPDATED, event.getCourseId());

            kafkaTemplate.send(KafkaTopics.COURSE_ENROLLMENT_UPDATED, event.getCourseId(), event);
            log.info("📤 Successfully sent CourseEnrollmentUpdatedEvent to Kafka for course: {}", event.getCourseId());

        } catch (Exception e) {
            log.error("❌ CRITICAL ERROR publishing to Kafka after commit! Event delivery FAILED. Course: {}. Error: {}",
                    event.getCourseId(), e.getMessage(), e);
        }
    }
}
