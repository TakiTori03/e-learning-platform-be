package com.hust.aiservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hust.aiservice.service.SemanticCacheService;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.data.embedding.Embedding;
import com.hust.aiservice.dto.response.ChatResponse;
import com.hust.aiservice.dto.Citation;
import com.hust.aiservice.repository.SemanticCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SemanticCacheServiceImpl implements SemanticCacheService {

    private final StringRedisTemplate redisTemplate;
    private final SemanticCacheRepository semanticCacheRepository;
    private final ObjectMapper objectMapper;
    private final EmbeddingModel embeddingModel;

    @Value("${rag.redis-exact-prefix:cache:exact:}")
    private String redisExactPrefix;

    @Value("${rag.cache-ttl-hours:24}")
    private long cacheTtlHours;

    @Value("${rag.semantic-cache-threshold:0.05}")
    private double semanticCacheThreshold;

    /**
     * Tra cứu cache: Đầu tiên tìm chính xác trên Redis (L1), sau đó tìm tương đồng trên Postgres (L2)
     */
    public ChatResponse getCachedResponse(String courseId, String query) {
        String resolvedCourseId = (courseId == null || courseId.isBlank()) ? null : courseId;
        // 1. Level 1: Exact Match in Redis
        String exactKey = redisExactPrefix + (resolvedCourseId == null ? "global" : resolvedCourseId) + ":" + query.trim().toLowerCase();
        try {
            String cachedJson = redisTemplate.opsForValue().get(exactKey);
            if (cachedJson != null) {
                log.info("🚀 [L1 Cache Hit] Redis exact match for query: {}", query);
                return deserializeResponse(cachedJson);
            }
        } catch (Exception e) {
            log.warn("⚠️ Lỗi truy vấn L1 Cache (Redis): {}", e.getMessage());
        }

        // 2. Level 2: Semantic Match in Postgres using pgvector
        try {
            Embedding embedding = embeddingModel.embed(query).content();
            String embeddingString = embedding.vectorAsList().toString();

            // Tìm câu hỏi có khoảng cách Cosine <= threshold
            List<Map<String, Object>> results = semanticCacheRepository.findSimilarQuery(embeddingString, resolvedCourseId, semanticCacheThreshold);

            if (!results.isEmpty()) {
                Map<String, Object> row = results.get(0);
                Long cacheId = ((Number) row.get("id")).longValue();
                String cachedQuery = getAsString(row.get("query"));
                String answer = getAsString(row.get("answer"));
                String citationsJson = getAsString(row.get("citations"));
                double distance = ((Number) row.get("distance")).doubleValue();

                log.info("🚀 [L2 Cache Hit] Postgres semantic match (similarity={}%): '{}' matched with '{}'",
                        String.format("%.1f", (1.0 - distance) * 100), query, cachedQuery);

                try {
                    semanticCacheRepository.updateHitCount(cacheId);
                } catch (Exception ex) {
                    log.warn("⚠️ Không thể cập nhật lượt truy cập cache: {}", ex.getMessage());
                }

                List<Citation> citations = Collections.emptyList();
                if (citationsJson != null) {
                    try {
                        citations = objectMapper.readValue(citationsJson,
                                objectMapper.getTypeFactory().constructCollectionType(List.class, Citation.class));
                    } catch (Exception e) {
                        log.error("⚠️ Lỗi deserialize citations từ L2 cache: {}", e.getMessage());
                    }
                }

                ChatResponse response = ChatResponse.builder()
                        .answer(answer)
                        .citations(citations)
                        .build();

                // Lưu ngược lại L1 (Redis) để lần sau truy cập nhanh hơn
                saveToL1(exactKey, response);

                return response;
            }
        } catch (Exception e) {
            log.warn("⚠️ Lỗi truy vấn L2 Cache (Postgres): {}", e.getMessage());
        }

        return null; // Cache Miss
    }

    /**
     * Lưu kết quả mới vào cả L1 (Redis) và L2 (Postgres)
     */
    public void saveToCache(String courseId, String query, ChatResponse response) {
        if (response == null || response.getAnswer() == null) {
            return;
        }

        String resolvedCourseId = (courseId == null || courseId.isBlank()) ? null : courseId;
        String exactKey = redisExactPrefix + (resolvedCourseId == null ? "global" : resolvedCourseId) + ":" + query.trim().toLowerCase();

        // 1. Lưu L1 Redis
        saveToL1(exactKey, response);

        // 2. Lưu L2 Postgres
        try {
            Embedding embedding = embeddingModel.embed(query).content();
            String embeddingString = embedding.vectorAsList().toString();
            String citationsJson = objectMapper.writeValueAsString(response.getCitations());

            semanticCacheRepository.insertCache(resolvedCourseId, query, response.getAnswer(), citationsJson, embeddingString, "langchain4j-embedding");
            log.info("💾 Đã lưu Q&A vào L2 Cache (Postgres) cho câu hỏi: '{}'", query);
        } catch (Exception e) {
            log.warn("⚠️ Không thể lưu Q&A vào L2 Cache (Postgres): {}", e.getMessage());
        }
    }

    private void saveToL1(String key, ChatResponse response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(key, json, cacheTtlHours, TimeUnit.HOURS);
            log.info("💾 Đã lưu Q&A vào L1 Cache (Redis)");
        } catch (Exception e) {
            log.warn("⚠️ Không thể lưu Q&A vào L1 Cache (Redis): {}", e.getMessage());
        }
    }

    private ChatResponse deserializeResponse(String json) {
        try {
            return objectMapper.readValue(json, ChatResponse.class);
        } catch (Exception e) {
            log.error("⚠️ Lỗi deserialize ChatResponse: {}", e.getMessage());
            return null;
        }
    }

    private String getAsString(Object obj) {
        if (obj == null) return null;
        if (obj instanceof org.postgresql.util.PGobject pgObj) {
            return pgObj.getValue();
        }
        return obj.toString();
    }

    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 3 * * *")
    public void cleanExpiredSemanticCache() {
        log.info("🧹 Semantic Cache Cleanup: Starting daily cleanup of old vector cache entries");
        try {
            java.time.Instant cutoff = java.time.Instant.now().minus(30, java.time.temporal.ChronoUnit.DAYS);
            semanticCacheRepository.deleteByLastAccessedAtBefore(cutoff);
            log.info("🧹 Semantic Cache Cleanup: Completed successfully. Evicted entries older than {}", cutoff);
        } catch (Exception e) {
            log.error("❌ Semantic Cache Cleanup Failed: {}", e.getMessage());
        }
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void clearCache(String courseId) {
        String resolvedCourseId = (courseId == null || courseId.isBlank()) ? "ALL_COURSES" : courseId;
        log.info("🧹 Bắt đầu dọn dẹp L1/L2 cache cho: {}", resolvedCourseId);

        // 1. Dọn dẹp L1 Cache (Redis)
        try {
            String pattern;
            if (courseId == null || courseId.isBlank()) {
                pattern = redisExactPrefix + "*"; // Xóa toàn bộ
            } else {
                pattern = redisExactPrefix + courseId + ":*"; // Chỉ xóa của courseId cụ thể
            }
            java.util.Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("🧹 L1 (Redis) Cache: Đã xóa {} keys với mẫu: {}", keys.size(), pattern);
            } else {
                log.info("🧹 L1 (Redis) Cache: Không tìm thấy key nào để xóa với mẫu: {}", pattern);
            }
        } catch (Exception e) {
            log.error("❌ Không thể dọn dẹp L1 Cache (Redis): {}", e.getMessage());
        }

        // 2. Dọn dẹp L2 Cache (Postgres Vector)
        try {
            semanticCacheRepository.deleteByCourseId(courseId);
            log.info("🧹 L2 (Postgres pgvector) Cache: Đã xóa các câu trả lời của: {}", resolvedCourseId);
        } catch (Exception e) {
            log.error("❌ Không thể dọn dẹp L2 Cache (Postgres Vector): {}", e.getMessage());
        }
    }
}
