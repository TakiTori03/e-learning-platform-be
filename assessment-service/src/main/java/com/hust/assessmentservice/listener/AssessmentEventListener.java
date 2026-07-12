package com.hust.assessmentservice.listener;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.hust.commonlibrary.constants.KafkaTopics;
import com.hust.commonlibrary.event.AssessmentSubmittedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AssessmentEventListener {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAssessmentSubmittedEvent(AssessmentSubmittedEvent event) {
        log.info("💡 Transaction COMMITTED successfully! Thread [{}] is picking up local AssessmentSubmittedEvent: attemptId={}, isPassed={}",
                Thread.currentThread().getName(), event.getAttemptId(), event.getIsPassed());

        try {
            log.info("🚀 Broadcasting event to Kafka topic '{}' for attempt: {}",
                    KafkaTopics.ASSESSMENT_SUBMITTED, event.getAttemptId());

            kafkaTemplate.send(KafkaTopics.ASSESSMENT_SUBMITTED, event.getUserId(), event);
            log.info("📤 Successfully sent AssessmentSubmittedEvent to Kafka for attempt: {}", event.getAttemptId());

        } catch (Exception e) {
            log.error("❌ CRITICAL ERROR publishing to Kafka after commit! Event delivery FAILED. Attempt: {}. Error: {}",
                    event.getAttemptId(), e.getMessage(), e);
        }
    }
}
