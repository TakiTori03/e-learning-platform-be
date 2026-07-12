package com.hust.mediaservice.listener;

import com.hust.commonlibrary.constants.KafkaTopics;
import com.hust.mediaservice.event.MediaProcessingRequestSpringEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class MediaEventListener {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleMediaProcessingRequest(MediaProcessingRequestSpringEvent springEvent) {
        var kafkaPayload = springEvent.getKafkaPayload();

        kafkaTemplate.send(KafkaTopics.MEDIA_PROCESSING, kafkaPayload.getMediaId(), kafkaPayload);
        log.info("🚀 Successfully sent MediaProcessingRequestEvent to Kafka for media: {}", kafkaPayload.getMediaId());
    }
}
