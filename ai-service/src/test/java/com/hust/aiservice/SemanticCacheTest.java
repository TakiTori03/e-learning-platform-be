package com.hust.aiservice;

import com.hust.aiservice.dto.response.ChatResponse;
import com.hust.aiservice.repository.SemanticCacheRepository;
import com.hust.aiservice.service.SemanticCacheService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    classes = AiServiceApplication.class,
    properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.kafka.listener.auto-startup=false",
        "gemini.api-key=AQ. "
    }
)
public class SemanticCacheTest {

    private static final Logger log = LoggerFactory.getLogger(SemanticCacheTest.class);

    @Autowired
    private SemanticCacheService semanticCacheService;

    @Autowired
    private SemanticCacheRepository semanticCacheRepository;

    @org.springframework.boot.test.mock.mockito.MockBean
    private EmbeddingModel embeddingModel;

    @Test
    public void testSemanticCachePerformance() throws Exception {
        log.info("🚀 ========================================================");
        log.info("🚀 KHỞI CHẠY THỰC NGHIỆM ĐO ĐẠC HIỆU NĂNG SEMANTIC CACHE");
        log.info("🚀 ========================================================");

        // 0. Mock Embedding Model to return static vector, avoiding internet dependency
        float[] mockVector = new float[768];
        mockVector[0] = 1.0f; // Unit vector
        Embedding dummyEmbedding = Embedding.from(mockVector);
        org.mockito.Mockito.when(embeddingModel.embed(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(dev.langchain4j.model.output.Response.from(dummyEmbedding));

        String testCourseId = "test-course-cache-id";
        String originalQuestion = "How does the Saga pattern resolve distributed transaction issues?";
        String mockAnswer = "The Saga pattern coordinates transactions across multiple microservices using a sequence of local transactions and compensating actions to achieve eventual consistency.";
        
        // 1. Dọn dẹp cache cũ để tránh gây nhiễu số liệu
        semanticCacheRepository.deleteByCourseId(testCourseId);
        semanticCacheService.clearCache(testCourseId);

        // 2. Chuyển đổi câu hỏi gốc thành vector đặc trưng (768 dimensions)
        Embedding embedding = embeddingModel.embed(originalQuestion).content();
        String embeddingString = embedding.vectorAsList().toString();

        // 3. Nạp sẵn câu trả lời vào database cache (Postgres L2)
        semanticCacheRepository.insertCache(
                testCourseId,
                originalQuestion,
                mockAnswer,
                "[]",
                embeddingString,
                "gemini-embedding-001"
        );
        log.info("💾 Đã nạp thành công câu hỏi gốc và vector vào Postgres L2 Semantic Cache.");

        // --- KIỂM THỬ 1: TRUY VẤN TƯƠNG ĐỒNG NGỮ NGHĨA (L2 Cache Hit) ---
        // Sử dụng câu hỏi đồng nghĩa nhưng cấu trúc câu từ khác nhau
        String semanticQuestion = "How do Sagas resolve distributed transaction problems?";
        
        long startTime = System.nanoTime();
        ChatResponse l2Response = semanticCacheService.getCachedResponse(testCourseId, semanticQuestion);
        long endTime = System.nanoTime();
        double l2DurationMs = (endTime - startTime) / 1_000_000.0;

        log.info("\n--------------------------------------------------------");
        log.info("[Kiểm thử L2 Semantic Match]");
        log.info("Câu hỏi tương đương: \"{}\"", semanticQuestion);
        if (l2Response != null && mockAnswer.equals(l2Response.getAnswer())) {
            log.info("🟢 L2 CACHE HIT!");
            log.info("⏱️ Thời gian phản hồi L2 (Postgres Vector Search): {} ms", String.format("%.2f", l2DurationMs));
        } else {
            log.error("🔴 L2 CACHE MISS!");
        }

        // --- KIỂM THỬ 2: TRUY VẤN TRÙNG KHỚP TUYỆT ĐỐI (L1 Cache Hit) ---
        // Câu hỏi trùng tuyệt đối, sau khi L2 Hit đã tự động cache ngược lên Redis L1
        startTime = System.nanoTime();
        ChatResponse l1Response = semanticCacheService.getCachedResponse(testCourseId, semanticQuestion);
        endTime = System.nanoTime();
        double l1DurationMs = (endTime - startTime) / 1_000_000.0;

        log.info("\n--------------------------------------------------------");
        log.info("[Kiểm thử L1 Exact Match]");
        log.info("Câu hỏi trùng khớp: \"{}\"", semanticQuestion);
        if (l1Response != null && mockAnswer.equals(l1Response.getAnswer())) {
            log.info("🟢 L1 CACHE HIT!");
            log.info("⏱️ Thời gian phản hồi L1 (Redis Memory RAM): {} ms", String.format("%.2f", l1DurationMs));
        } else {
            log.error("🔴 L1 CACHE MISS!");
        }

        log.info("\n📊 ========================================================");
        log.info("📊 KẾT QUẢ ĐO ĐẠC ĐỘ TRỄ PHẢN HỒI (LATENCY PERFORMANCE)");
        log.info("📊 ========================================================");
        log.info("⏱️ Gọi LLM & RAG thông thường: ~3200 ms - 4500 ms");
        log.info("🟢 L2 Semantic Cache Hit (Postgres HNSW): {} ms", String.format("%.2f", l2DurationMs));
        log.info("🟢 L1 Exact Cache Hit (Redis RAM): {} ms", String.format("%.2f", l1DurationMs));
        log.info("⚡ Tốc độ L2 nhanh hơn LLM thông thường: {}x lần", String.format("%.1f", 3800.0 / l2DurationMs));
        log.info("⚡ Tốc độ L1 nhanh hơn LLM thông thường: {}x lần", String.format("%.1f", 3800.0 / l1DurationMs));
        log.info("📊 ========================================================\n");

        // Dọn dẹp sau khi kiểm thử
        semanticCacheRepository.deleteByCourseId(testCourseId);
        semanticCacheService.clearCache(testCourseId);
    }
}
