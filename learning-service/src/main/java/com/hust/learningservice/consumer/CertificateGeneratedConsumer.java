package com.hust.learningservice.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.hust.commonlibrary.constants.KafkaTopics;
import com.hust.commonlibrary.event.CertificateGeneratedEvent;
import com.hust.learningservice.service.CertificateService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class CertificateGeneratedConsumer {

    private final CertificateService certificateService;

    @KafkaListener(topics = KafkaTopics.CERTIFICATE_GENERATED, groupId = "learning-certificate-group")
    public void consumeCertificateGeneratedEvent(CertificateGeneratedEvent event) {
        log.info("Received CertificateGeneratedEvent from Kafka: {}", event);
        try {
            certificateService.handleCertificateGenerated(event);
        } catch (Exception e) {
            log.error("Failed to process CertificateGeneratedEvent for certId: {}. Error: {}",
                    event.getCertificateId(), e.getMessage(), e);
        }
    }
}
