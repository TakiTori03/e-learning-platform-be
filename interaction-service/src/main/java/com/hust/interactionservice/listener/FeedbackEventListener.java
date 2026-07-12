package com.hust.interactionservice.listener;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.hust.commonlibrary.constants.KafkaTopics;
import com.hust.commonlibrary.event.FeedbackRepliedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class FeedbackEventListener {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleFeedbackRepliedEvent(FeedbackRepliedEvent event) {
        log.info("💡 Thread [{}] is picking up local FeedbackRepliedEvent for feedback: {}",
                Thread.currentThread().getName(), event.getFeedbackId());

        try {
            log.info("🚀 Broadcasting event to Kafka topic '{}' for feedback: {}",
                    KafkaTopics.FEEDBACK_REPLIED, event.getFeedbackId());

            kafkaTemplate.send(KafkaTopics.FEEDBACK_REPLIED, event.getFeedbackId(), event);
            log.info("📤 Successfully sent FeedbackRepliedEvent to Kafka for feedback: {}", event.getFeedbackId());

        } catch (Exception e) {
            log.error("❌ CRITICAL ERROR publishing to Kafka! Event delivery FAILED. Feedback: {}. Error: {}",
                    event.getFeedbackId(), e.getMessage(), e);
        }
    }
}
