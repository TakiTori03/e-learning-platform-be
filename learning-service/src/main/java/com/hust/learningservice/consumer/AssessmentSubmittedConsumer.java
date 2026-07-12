package com.hust.learningservice.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.hust.commonlibrary.constants.KafkaTopics;
import com.hust.commonlibrary.entity.QuizType;
import com.hust.commonlibrary.event.AssessmentSubmittedEvent;
import com.hust.learningservice.service.CertificateService;
import com.hust.learningservice.service.LearningService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AssessmentSubmittedConsumer {

    private final LearningService learningService;
    private final CertificateService certificateService;

    @KafkaListener(topics = KafkaTopics.ASSESSMENT_SUBMITTED, groupId = "learning-group")
    public void consumeAssessmentSubmittedEvent(AssessmentSubmittedEvent event) {
        log.info("Received AssessmentSubmittedEvent from Kafka: {}", event);

        if (event.getIsPassed() != null && event.getIsPassed()) {
            // Check if it's the final exam (either quizType is FINAL or lessonId is null/blank)
            if (QuizType.FINAL.equals(event.getQuizType()) || event.getLessonId() == null || event.getLessonId().isBlank()) {
                try {
                    log.info("🏆 Final Quiz passed! Triggering certificate issuance for userId={}, courseId={}, score={}",
                            event.getUserId(), event.getCourseId(), event.getScore());
                    certificateService.issueCertificate(event.getUserId(), event.getCourseId(), event.getScore());
                    log.info("Successfully initiated certificate issuance for userId={}, courseId={}", event.getUserId(), event.getCourseId());
                } catch (Exception e) {
                    log.error("Failed to initiate certificate issuance for userId={}, courseId={}. Error: {}",
                            event.getUserId(), event.getCourseId(), e.getMessage(), e);
                }
            } else {
                try {
                    log.info("Quiz passed! Syncing progress for userId={}, lessonId={}", event.getUserId(), event.getLessonId());
                    learningService.completeQuizLesson(event.getUserId(), event.getLessonId());
                    log.info("Successfully completed quiz lesson progress sync for userId={}, lessonId={}", event.getUserId(), event.getLessonId());
                } catch (Exception e) {
                    log.error("Failed to complete quiz lesson progress for userId={}, lessonId={}. Error: {}",
                            event.getUserId(), event.getLessonId(), e.getMessage(), e);
                }
            }
        } else {
            log.info("Quiz did not pass (isPassed=false/null) for attemptId={}. Skipping progress sync.", event.getAttemptId());
        }
    }
}
