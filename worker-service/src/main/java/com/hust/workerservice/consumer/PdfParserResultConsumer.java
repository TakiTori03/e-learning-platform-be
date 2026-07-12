package com.hust.workerservice.consumer;

import com.hust.commonlibrary.constants.KafkaTopics;
import com.hust.commonlibrary.entity.ContentType;
import com.hust.commonlibrary.event.LessonMediaReadyEvent;
import com.hust.commonlibrary.event.PdfParserResultEvent;
import com.hust.commonlibrary.event.RawTextIngestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Consumer lắng nghe kết quả bóc tách PDF từ Python pdf-parser-service qua Kafka.
 *
 * Luồng xử lý:
 * 1. Nhận danh sách các trang đã OCR + phân mảnh từ topic pdf-parser-results.
 * 2. Duyệt qua từng trang, gửi các chunk văn bản lên topic raw-text-ingested.
 * 3. Phát sự kiện LessonMediaReadyEvent báo hoàn thành.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class PdfParserResultConsumer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(
            topics = KafkaTopics.PDF_PARSER_RESULTS,
            groupId = "pdf-result-worker-group",
            concurrency = "1",
            properties = {"spring.json.value.default.type=com.hust.commonlibrary.event.PdfParserResultEvent"}
    )
    public void consume(PdfParserResultEvent event) {
        log.info("📄 Nhận kết quả PDF Parser từ Python cho media: {} (success={})", event.getMediaId(), event.isSuccess());

        if (!event.isSuccess()) {
            log.error("❌ PDF Parser thất bại cho media {}: {}", event.getMediaId(), event.getErrorMessage());
            emitFailedEvent(event);
            return;
        }

        try {
            // 1. Duyệt qua từng trang và gửi các Semantic Chunks lên Kafka
            if (event.getPages() != null) {
                int chunkIdx = 0;
                for (PdfParserResultEvent.ParsedPage page : event.getPages()) {
                    if (page.getChunks() != null) {
                        for (String chunkContent : page.getChunks()) {
                            if (chunkContent != null && !chunkContent.trim().isEmpty()) {
                                RawTextIngestedEvent ingestionEvent = RawTextIngestedEvent.builder()
                                        .courseId(event.getCourseId())
                                        .lessonId(event.getLessonId())
                                        .mediaId(event.getMediaId())
                                        .content(chunkContent.trim())
                                        .contentType(ContentType.PDF)
                                        .sourceCitation("Trang " + page.getPage())
                                        .chunkIndex(chunkIdx++)
                                        .build();

                                kafkaTemplate.send(KafkaTopics.RAW_TEXT_INGESTED, event.getLessonId(), ingestionEvent);
                            }
                        }
                    }
                }
                log.info("✅ Đã phát tất cả RawTextIngestedEvent (PDF) cho lesson {}. Tổng số trang: {}",
                        event.getLessonId(), event.getTotalPages());
            }

            // 2. Phát sự kiện LessonMediaReadyEvent báo hoàn thành
            LessonMediaReadyEvent readyEvent = LessonMediaReadyEvent.builder()
                    .lessonId(event.getLessonId())
                    .mediaId(event.getMediaId())
                    .url(event.getFileUrl())
                    .fileSize(0L)
                    .build();

            kafkaTemplate.send(KafkaTopics.LESSON_MEDIA_READY, event.getLessonId(), readyEvent);
            log.info("🎉 Hoàn tất toàn bộ chu trình xử lý PDF & Gửi Ingestion Event cho lesson {}", event.getLessonId());

        } catch (Exception e) {
            log.error("❌ Lỗi khi xử lý kết quả PDF Parser cho media {}: {}", event.getMediaId(), e.getMessage(), e);
            emitFailedEvent(event);
        }
    }

    private void emitFailedEvent(PdfParserResultEvent event) {
        try {
            LessonMediaReadyEvent failedEvent = LessonMediaReadyEvent.builder()
                    .lessonId(event.getLessonId())
                    .mediaId(event.getMediaId())
                    .url(null)
                    .fileSize(-1L)
                    .build();
            kafkaTemplate.send(KafkaTopics.LESSON_MEDIA_READY, event.getLessonId(), failedEvent);
            log.warn("📢 Đã gửi LessonMediaReadyEvent báo lỗi (FAILED) cho lesson {}", event.getLessonId());
        } catch (Exception e) {
            log.error("Lỗi khi phát sự kiện thất bại: {}", e.getMessage(), e);
        }
    }


}
