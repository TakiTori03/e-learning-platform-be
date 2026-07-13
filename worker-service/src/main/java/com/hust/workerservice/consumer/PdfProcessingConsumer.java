package com.hust.workerservice.consumer;

import com.hust.commonlibrary.event.LessonMediaReadyEvent;
import com.hust.commonlibrary.event.MediaProcessingRequestEvent;
import com.hust.commonlibrary.event.PdfParserRequestEvent;
import com.hust.commonlibrary.annotation.TrackPerformance;
import com.hust.commonlibrary.constants.KafkaTopics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.retry.annotation.Backoff;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Consumer xử lý sự kiện PDF từ Kafka.
 *
 * Luồng mới (Kafka-driven):
 * 1. Nhận sự kiện media-processing (DOCUMENT).
 * 2. Thay vì gửi REST POST multipart tới Python, chỉ cần phát PdfParserRequestEvent
 *    chứa fileUrl (MinIO key) lên Kafka topic pdf-parser-requests.
 * 3. Python pdf-parser-service sẽ tự tải file từ MinIO, OCR, và trả kết quả
 *    qua topic pdf-parser-results.
 * 4. PdfParserResultConsumer sẽ nhận kết quả và phát RawTextIngestedEvent + LessonMediaReadyEvent.
 *
 * Lợi ích: Giải phóng Thread Java ngay lập tức, kiểm soát tải GPU hoàn toàn.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PdfProcessingConsumer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 5000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            include = {
                java.lang.RuntimeException.class
            }
    )
    @KafkaListener(topics = KafkaTopics.MEDIA_PROCESSING, groupId = "pdf-worker-group", concurrency = "1")
    @TrackPerformance(threshold = 30000, description = "PDF Parser Kafka Request Dispatch")
    public void consume(MediaProcessingRequestEvent event) {
        if (!"DOCUMENT".equalsIgnoreCase(event.getMediaType())) {
            return;
        }

        log.info("📄 Nhận sự kiện xử lý PDF từ Kafka: {}", event);

        try {
            // Phát sự kiện yêu cầu bóc tách PDF qua Kafka (thay vì REST)
            PdfParserRequestEvent parserRequest = PdfParserRequestEvent.builder()
                    .mediaId(event.getMediaId())
                    .courseId(event.getCourseId())
                    .lessonId(event.getLessonId())
                    .fileUrl(event.getFileUrl())
                    .build();

            kafkaTemplate.send(KafkaTopics.PDF_PARSER_REQUESTS, event.getLessonId(), parserRequest);
            log.info("📨 Đã phát PdfParserRequestEvent qua Kafka cho Python Parser bóc tách bất đồng bộ. FileUrl: {}", event.getFileUrl());

        } catch (Exception e) {
            log.error("❌ Lỗi khi gửi yêu cầu PDF Parser qua Kafka cho media {}: {}", event.getMediaId(), e.getMessage(), e);
            throw new RuntimeException("Failed to dispatch PDF parser request to Kafka.", e);
        }
    }

    @DltHandler
    public void handleDlt(MediaProcessingRequestEvent event, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        if (!"DOCUMENT".equalsIgnoreCase(event.getMediaType())) {
            return;
        }
        log.error("❌ [DLQ] Xử lý PDF cho lesson {} thất bại hoàn toàn sau tất cả các lần thử lại tại topic: {}", event.getLessonId(), topic);
        try {
            LessonMediaReadyEvent failedEvent = LessonMediaReadyEvent.builder()
                    .lessonId(event.getLessonId())
                    .mediaId(event.getMediaId())
                    .url(null)
                    .fileSize(-1L)
                    .build();
            kafkaTemplate.send(KafkaTopics.LESSON_MEDIA_READY, event.getLessonId(), failedEvent);
            log.warn("📢 Đã gửi LessonMediaReadyEvent báo lỗi (status: FAILED) cho lesson {}", event.getLessonId());
        } catch (Exception e) {
            log.error("Lỗi khi xử lý DLQ Handler: {}", e.getMessage(), e);
        }
    }
}
