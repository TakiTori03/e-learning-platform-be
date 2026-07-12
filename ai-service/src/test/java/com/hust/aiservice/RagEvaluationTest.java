package com.hust.aiservice;

import com.hust.aiservice.entity.DocumentChunk;
import com.hust.aiservice.repository.DocumentChunkRepository;
import com.hust.aiservice.dto.SearchResultDTO;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest(
    classes = AiServiceApplication.class,
    properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.kafka.listener.auto-startup=false",
        "gemini.api-key="
    }
)
public class RagEvaluationTest {

    private static final Logger log = LoggerFactory.getLogger(RagEvaluationTest.class);

    @Autowired
    private DocumentChunkRepository documentChunkRepository;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Value("${rag.search-limit:24}")
    private int searchLimit;

    @Value("${rag.rrf-k:60.0}")
    private double rrfK;

    @Value("${rag.top-k-context:3}")
    private int topK;

    @Test
    public void evaluateRagQuality() {
        log.info("🚀 ========================================================");
        log.info("🚀 KHỞI CHẠY THỰC NGHIỆM ĐO ĐẠC CHẤT LƯỢNG RETRIEVAL RAG");
        log.info("🚀 ========================================================");

        // 1. Lấy mẫu 50 chunks ngẫu nhiên từ database để làm tập dữ liệu kiểm thử
        Page<DocumentChunk> chunkPage = documentChunkRepository.findAll(PageRequest.of(0, 50));
        List<DocumentChunk> chunks = chunkPage.getContent();

        if (chunks.isEmpty()) {
            log.warn("⚠️ Database trống (không có chunks nào). Vui lòng tải tài liệu khóa học lên trước!");
            return;
        }

        int totalQueries = chunks.size();
        int vectorHits = 0;
        int hybridHits = 0;

        log.info("📊 Tập dữ liệu thực nghiệm: {} câu hỏi (được trích xuất tự động từ tài liệu)", totalQueries);

        for (int idx = 0; idx < chunks.size(); idx++) {
            DocumentChunk targetChunk = chunks.get(idx);
            String courseId = targetChunk.getCourseId();
            String fullText = targetChunk.getContent();

            // Tạo câu hỏi thực nghiệm bằng cách trích xuất 1 cụm từ ngẫu nhiên khoảng 6-8 từ ở giữa chunk
            String queryText = generateQueryFromContent(fullText);
            if (queryText.isBlank()) {
                totalQueries--;
                continue;
            }

            log.info("\n--------------------------------------------------------");
            log.info("[Câu hỏi {}/{}] Query: \"{}\"", idx + 1, chunks.size(), queryText);
            log.info("🎯 Ground Truth Chunk ID: {}", targetChunk.getId());

            // --- LUỒNG 1: CHỈ VẼ VECTOR SEARCH ---
            List<SearchResultDTO> vectorResults = runVectorSearch(courseId, queryText);
            boolean isVectorHit = isChunkRetrieved(targetChunk.getId(), vectorResults, topK);
            if (isVectorHit) {
                vectorHits++;
                log.info("🟢 [Vector Search Only] -> HIT (Tìm thấy trong Top {})", topK);
            } else {
                log.info("🔴 [Vector Search Only] -> MISS");
            }

            // --- LUỒNG 2: CHẠY HYBRID SEARCH (VECTOR + FTS + RRF) ---
            List<SearchResultDTO> ftsResults = runFtsSearch(courseId, queryText);
            List<SearchResultDTO> hybridResults = performRRF(vectorResults, ftsResults, topK);
            boolean isHybridHit = isChunkRetrieved(targetChunk.getId(), hybridResults, topK);
            if (isHybridHit) {
                hybridHits++;
                log.info("🟢 [Hybrid RAG + RRF] -> HIT (Tìm thấy trong Top {})", topK);
            } else {
                log.info("🔴 [Hybrid RAG + RRF] -> MISS");
            }
        }

        // 2. Tính toán và in kết quả thống kê Hit Rate / Recall
        double vectorAccuracy = ((double) vectorHits / totalQueries) * 100.0;
        double hybridAccuracy = ((double) hybridHits / totalQueries) * 100.0;

        log.info("\n📊 ========================================================");
        log.info("📊 KẾT QUẢ THỰC NGHIỆM ĐO ĐẠC ĐỘ CHÍNH XÁC THU HỒI (RECALL@{})", topK);
        log.info("📊 ========================================================");
        log.info("Tổng số câu hỏi đánh giá: {}", totalQueries);
        log.info("🟢 Độ chính xác luồng Vector Search đơn lẻ: {}% ({} / {})", 
                String.format("%.2f", vectorAccuracy), vectorHits, totalQueries);
        log.info("🟢 Độ chính xác luồng Hybrid RAG (Vector + FTS + RRF): {}% ({} / {})", 
                String.format("%.2f", hybridAccuracy), hybridHits, totalQueries);
        log.info("📈 Tỷ lệ cải thiện hiệu năng truy xuất: +{}%", 
                String.format("%.2f", (hybridAccuracy - vectorAccuracy)));
        log.info("📊 ========================================================\n");
    }

    private List<SearchResultDTO> runVectorSearch(String courseId, String query) {
        try {
            Embedding embedding = embeddingModel.embed(query).content();
            String embeddingString = embedding.vectorAsList().toString();
            List<Object[]> rawVector = documentChunkRepository.vectorSearch(courseId, embeddingString, searchLimit);
            return mapToSearchResults(rawVector);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<SearchResultDTO> runFtsSearch(String courseId, String query) {
        try {
            List<Object[]> rawFts = documentChunkRepository.ftsSearch(courseId, query, searchLimit);
            return mapToSearchResults(rawFts);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<SearchResultDTO> performRRF(List<SearchResultDTO> vectorResults, List<SearchResultDTO> ftsResults, int limit) {
        Map<String, Double> rrfScores = new HashMap<>();
        Map<String, SearchResultDTO> documentMap = new HashMap<>();

        calPoint(vectorResults, rrfScores, documentMap);
        calPoint(ftsResults, rrfScores, documentMap);

        return rrfScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> documentMap.get(entry.getKey()))
                .toList();
    }

    private void calPoint(List<SearchResultDTO> results, Map<String, Double> rrfScores, Map<String, SearchResultDTO> documentMap) {
        for (int i = 0; i < results.size(); i++) {
            SearchResultDTO doc = results.get(i);
            double rank = i + 1.0;
            rrfScores.put(doc.getId(), rrfScores.getOrDefault(doc.getId(), 0.0) + (1.0 / (rrfK + rank)));
            documentMap.putIfAbsent(doc.getId(), doc);
        }
    }

    private boolean isChunkRetrieved(String targetChunkId, List<SearchResultDTO> results, int topKLimit) {
        int limit = Math.min(results.size(), topKLimit);
        for (int i = 0; i < limit; i++) {
            if (results.get(i).getId().equals(targetChunkId)) {
                return true;
            }
        }
        return false;
    }

    private String generateQueryFromContent(String text) {
        if (text == null || text.isBlank()) return "";
        String[] words = text.split("\\s+");
        if (words.length <= 10) {
            return String.join(" ", words);
        }
        // Chọn 6 từ ở khoảng giữa văn bản để làm query tìm kiếm
        int startIdx = words.length / 3;
        int length = Math.min(7, words.length - startIdx);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(words[startIdx + i]).append(" ");
        }
        return sb.toString().trim()
                .replaceAll("[.,;:!?\"'()\\-]", ""); // Làm sạch ký tự đặc biệt
    }

    private List<SearchResultDTO> mapToSearchResults(List<Object[]> rawResults) {
        List<SearchResultDTO> results = new ArrayList<>();
        if (rawResults == null) return results;

        for (Object[] row : rawResults) {
            results.add(SearchResultDTO.builder()
                    .id((String) row[0])
                    .courseId((String) row[1])
                    .lessonId((String) row[2])
                    .mediaId((String) row[3])
                    .chunkIndex(row[4] != null ? ((Number) row[4]).intValue() : null)
                    .content((String) row[5])
                    .sourceCitation((String) row[7])
                    .score(row[8] != null ? ((Number) row[8]).doubleValue() : 0.0)
                    .build());
        }
        return results;
    }
}
