package com.hust.learningservice.listener;

import com.hust.commonlibrary.constants.KafkaTopics;
import com.hust.commonlibrary.event.CertificateIssuedEvent;
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
public class CertificateEventListener {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCertificateIssuedEvent(CertificateIssuedEvent event) {
        log.info("💡 Transaction COMMITTED! Thread [{}] is processing CertificateIssuedEvent: certId={}",
                Thread.currentThread().getName(), event.getCertificateId());

        try {
            kafkaTemplate.send(KafkaTopics.CERTIFICATE_ISSUED, event.getUserId(), event);
            log.info("📤 Successfully sent CertificateIssuedEvent to Kafka topic '{}' for certId: {}",
                    KafkaTopics.CERTIFICATE_ISSUED, event.getCertificateId());
        } catch (Exception e) {
            log.error("❌ Failed to send CertificateIssuedEvent to Kafka! CertId: {}. Error: {}",
                    event.getCertificateId(), e.getMessage(), e);
        }
    }
}
