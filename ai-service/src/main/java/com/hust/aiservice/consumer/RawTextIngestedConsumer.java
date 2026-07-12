package com.hust.aiservice.consumer;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.data.embedding.Embedding;
import com.hust.commonlibrary.event.RawTextIngestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class RawTextIngestedConsumer {

    private final EmbeddingModel embeddingModel;
    private final JdbcTemplate jdbcTemplate;

    @KafkaListener(
            topics = "${spring.kafka.topic.raw-text-ingested:raw-text-ingested-topic}",
            groupId = "${spring.kafka.consumer.group-id:ai-group}"
    )
    public void consume(List<RawTextIngestedEvent> events) {
        log.info("📩 Nhận được BATCH {} RawTextIngestedEvent", events.size());
        
        try {
            // 1. Lọc ra các event hợp lệ
            List<RawTextIngestedEvent> validEvents = events.stream()
                .filter(e -> e.getContent() != null && !e.getContent().trim().isEmpty() && e.getContentType() != null)
                    .toList();

            if (validEvents.isEmpty()) {
                log.warn("⚠️ Batch không có event nào hợp lệ, bỏ qua.");
                return;
            }

            // 2. Chuyển đổi thành TextSegment để LangChain4j xử lý
            List<dev.langchain4j.data.segment.TextSegment> segments = validEvents.stream()
                .map(e -> dev.langchain4j.data.segment.TextSegment.from(e.getContent()))
                    .toList();

            // 3. Gọi Embedding API theo từng Sub-batch (kích thước 50) có kèm Retry
            List<Embedding> embeddings = new ArrayList<>();
            int subBatchSize = 50;
            
            for (int i = 0; i < segments.size(); i += subBatchSize) {
                List<dev.langchain4j.data.segment.TextSegment> subBatch = segments.subList(i, Math.min(i + subBatchSize, segments.size()));
                
                // Gọi API với cơ chế tự động Retry Exponential Backoff
                List<Embedding> subEmbeddings = callGeminiWithRetry(subBatch);
                embeddings.addAll(subEmbeddings);
                
                // Nghỉ ngắn để giãn cách cuộc gọi tránh chạm trần RPM của Gemini
                if (i + subBatchSize < segments.size()) {
                    try {
                        Thread.sleep(150);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            if (embeddings.size() != validEvents.size()) {
                throw new RuntimeException("Mismatch size: expected " + validEvents.size() + " embeddings but got " + embeddings.size());
            }

            // 4. Lưu từng chunk vào DB bằng Batch Update để tối ưu I/O
            String sql = "INSERT INTO document_chunks (id, course_id, lesson_id, media_id, chunk_index, content, content_type, source_citation, embedding, created_at) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, cast(? as vector), NOW())";

            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    RawTextIngestedEvent event = validEvents.get(i);
                    Embedding embeddingObj = embeddings.get(i);
                    
                    ps.setString(1, UUID.randomUUID().toString());
                    ps.setString(2, event.getCourseId());
                    ps.setString(3, event.getLessonId());
                    ps.setString(4, event.getMediaId());
                    
                    if (event.getChunkIndex() != null) {
                        ps.setInt(5, event.getChunkIndex());
                    } else {
                        ps.setNull(5, java.sql.Types.INTEGER);
                    }
                    
                    ps.setString(6, event.getContent());
                    ps.setString(7, event.getContentType().name());
                    ps.setString(8, event.getSourceCitation());
                    ps.setString(9, embeddingObj.vectorAsList().toString());
                }

                @Override
                public int getBatchSize() {
                    return validEvents.size();
                }
            });

            log.info("✅ Đã lưu thành công {} chunks vào pgvector DB", validEvents.size());

        } catch (Exception e) {
            log.error("❌ Lỗi xảy ra khi xử lý BATCH RawTextIngestedEvent: {}", e.getMessage(), e);
            throw new RuntimeException("Batch raw text ingestion process failed, triggering retry topic", e);
        }
    }

    private List<Embedding> callGeminiWithRetry(List<dev.langchain4j.data.segment.TextSegment> subBatch) {
        int maxAttempts = 3;
        long backoffMs = 1000;
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return embeddingModel.embedAll(subBatch).content();
            } catch (Exception e) {
                if (attempt == maxAttempts) {
                    log.error("❌ Gọi Gemini Embedding thất bại sau {} lần thử", maxAttempts);
                    throw e;
                }
                log.warn("⚠️ Lỗi gọi Gemini Embedding (Thử lần {}): {}. Đang thử lại sau {}ms...", attempt, e.getMessage(), backoffMs);
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                backoffMs *= 2;
            }
        }
        return new ArrayList<>();
    }

}

