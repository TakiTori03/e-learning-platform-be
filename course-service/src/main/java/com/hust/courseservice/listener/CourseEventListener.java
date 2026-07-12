package com.hust.courseservice.listener;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.hust.commonlibrary.constants.KafkaTopics;
import com.hust.commonlibrary.event.LessonQuizLinkedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class CourseEventListener {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLessonQuizLinkedEvent(LessonQuizLinkedEvent event) {
        log.info("💡 Course transaction COMMITTED! Thread [{}] is processing LessonQuizLinkedEvent: lessonId={}, quizId={}, action={}",
                Thread.currentThread().getName(), event.getLessonId(), event.getQuizId(), event.getAction());
        try {
            kafkaTemplate.send(KafkaTopics.LESSON_QUIZ_LINKED, event.getLessonId(), event);
            log.info("🚀 Successfully sent LessonQuizLinkedEvent to Kafka topic '{}'", KafkaTopics.LESSON_QUIZ_LINKED);
        } catch (Exception e) {
            log.error("❌ Failed to send LessonQuizLinkedEvent to Kafka! Error: {}", e.getMessage(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCourseSearchSyncEvent(com.hust.commonlibrary.event.CourseSearchSyncEvent event) {
        log.info("💡 Course transaction COMMITTED! Thread [{}] is processing CourseSearchSyncEvent: courseId={}, action={}",
                Thread.currentThread().getName(), event.getId(), event.getAction());
        try {
            kafkaTemplate.send(KafkaTopics.COURSE_SEARCH_SYNC, event.getId(), event);
            log.info("🚀 Successfully sent CourseSearchSyncEvent to Kafka topic '{}'", KafkaTopics.COURSE_SEARCH_SYNC);
        } catch (Exception e) {
            log.error("❌ Failed to send CourseSearchSyncEvent to Kafka! Error: {}", e.getMessage(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCourseFinalExamLinkedEvent(com.hust.commonlibrary.event.CourseFinalExamLinkedEvent event) {
        log.info("💡 Course transaction COMMITTED! Thread [{}] is processing CourseFinalExamLinkedEvent: courseId={}, finalExamId={}, action={}",
                Thread.currentThread().getName(), event.getCourseId(), event.getFinalExamId(), event.getAction());
        try {
            kafkaTemplate.send(KafkaTopics.COURSE_FINAL_EXAM_LINKED, event.getCourseId(), event);
            log.info("🚀 Successfully sent CourseFinalExamLinkedEvent to Kafka topic '{}'", KafkaTopics.COURSE_FINAL_EXAM_LINKED);
        } catch (Exception e) {
            log.error("❌ Failed to send CourseFinalExamLinkedEvent to Kafka! Error: {}", e.getMessage(), e);
        }
    }
}
