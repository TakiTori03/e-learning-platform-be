package com.hust.workerservice.consumer;

import com.hust.commonlibrary.constants.KafkaTopics;
import com.hust.commonlibrary.event.CertificateGeneratedEvent;
import com.hust.commonlibrary.event.CertificateIssuedEvent;
import com.hust.workerservice.service.CertificateGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CertificateIssuedConsumer {

    private final CertificateGenerationService certificateGenerationService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(
            topics = KafkaTopics.CERTIFICATE_ISSUED,
            groupId = "certificate-worker-group",
            concurrency = "2"
    )
    public void consume(CertificateIssuedEvent event) {
        log.info("📥 Received CertificateIssuedEvent from Kafka: certId={}, student={}, course={}",
                event.getCertificateId(), event.getStudentName(), event.getCourseName());

        try {
            String fileUrl = certificateGenerationService.generateAndUploadCertificate(event);

            CertificateGeneratedEvent responseEvent = CertificateGeneratedEvent.builder()
                    .certificateId(event.getCertificateId())
                    .certificateUrl(fileUrl)
                    .isSuccess(true)
                    .build();

            kafkaTemplate.send(KafkaTopics.CERTIFICATE_GENERATED, event.getCertificateId(), responseEvent);
            log.info("📤 Published CertificateGeneratedEvent to Kafka for certId={}", event.getCertificateId());

        } catch (Exception e) {
            log.error("❌ Failed to generate certificate for certId={}. Error: {}", event.getCertificateId(), e.getMessage(), e);

            CertificateGeneratedEvent errorEvent = CertificateGeneratedEvent.builder()
                    .certificateId(event.getCertificateId())
                    .certificateUrl(null)
                    .isSuccess(false)
                    .build();

            try {
                kafkaTemplate.send(KafkaTopics.CERTIFICATE_GENERATED, event.getCertificateId(), errorEvent);
                log.info("📤 Published failed CertificateGeneratedEvent to Kafka for certId={}", event.getCertificateId());
            } catch (Exception kafkaEx) {
                log.error("❌ Critical: Failed to send error event to Kafka for certId={}. Error: {}",
                        event.getCertificateId(), kafkaEx.getMessage());
            }
        }
    }
}
