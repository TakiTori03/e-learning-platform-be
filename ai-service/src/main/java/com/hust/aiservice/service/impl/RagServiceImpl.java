package com.hust.aiservice.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.hust.aiservice.dto.Citation;
import com.hust.aiservice.dto.SearchResultDTO;
import com.hust.aiservice.dto.response.ChatResponse;
import com.hust.aiservice.repository.DocumentChunkRepository;
import com.hust.aiservice.service.RagService;
import com.hust.commonlibrary.entity.ContentType;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagServiceImpl implements RagService {

    private final EmbeddingModel embeddingModel;
    private final DocumentChunkRepository documentChunkRepository;
    private final CohereRerankService cohereRerankService;

    @Value("${rag.search-limit}")
    private int searchLimit;

    @Value("${rag.top-k-context}")
    private int topKContext;

    @Value("${rag.rrf-k}")
    private double rrfK;

    @Value("${rag.rerank-top-k}")
    private int rerankTopK;

    @Value("${rag.context-window-size}")
    private int contextWindowSize;

    /**
     * Hàm lấy Vector thô từ câu hỏi
     */
    private String getEmbeddingString(String query) {
        Embedding embedding = embeddingModel.embed(query).content();
        return embedding.vectorAsList().toString();
    }
    /**
     * TỐI ƯU LANGCHAIN4J: Chỉ thực hiện Vector Search + FTS + Rerank và trả về Context thô (Raw Data).
     * KHÔNG gọi Gemini để tránh lãng phí (Double LLM Call).
     */
    public ChatResponse retrieveContext(String courseId, String query) {
        log.info("🔍 Bắt đầu RAG (Chỉ truy xuất context thô) cho Course ID: [{}], Query: [{}]", courseId, query);

        // 1. Quét tìm kiếm song song (Thực sự): Vector Search + Full-Text Search
        CompletableFuture<List<SearchResultDTO>> vectorFuture = CompletableFuture.supplyAsync(() -> {
            try {
                String embeddingString = getEmbeddingString(query);
                List<Object[]> rawVector = documentChunkRepository.vectorSearch(courseId, embeddingString, searchLimit);
                return mapToSearchResults(rawVector);
            } catch (Exception e) {
                log.error("⚠️ Lỗi quét Vector Search: {}", e.getMessage());
                return new ArrayList<>();
            }
        });

        CompletableFuture<List<SearchResultDTO>> ftsFuture = CompletableFuture.supplyAsync(() -> {
            try {
                List<Object[]> rawFts = documentChunkRepository.ftsSearch(courseId, query, searchLimit);
                return mapToSearchResults(rawFts);
            } catch (Exception e) {
                log.error("⚠️ Lỗi quét Full-Text Search: {}", e.getMessage());
                return new ArrayList<>();
            }
        });

        // Chờ cả 2 truy vấn hoàn thành (join)
        List<SearchResultDTO> vectorResults = vectorFuture.join();
        List<SearchResultDTO> ftsResults = ftsFuture.join();

        // 2. Thuật toán trộn RRF & Reranking
        List<SearchResultDTO> finalContext;
        if (cohereRerankService.isEnabled()) {
            List<SearchResultDTO> candidates = performRRF(vectorResults, ftsResults, searchLimit);
            finalContext = cohereRerankService.rerank(query, candidates, rerankTopK);
        } else {
            finalContext = performRRF(vectorResults, ftsResults, topKContext);
        }

        if (finalContext.isEmpty()) {
            return ChatResponse.builder()
                    .answer("Không có dữ liệu ngữ cảnh nào phù hợp trong khóa học.")
                    .citations(Collections.emptyList())
                    .build();
        }

        // 3. Ghép context thành chuỗi thô để trả về cho LangChain4j Agent
        StringBuilder rawContextBuilder = new StringBuilder();
        rawContextBuilder.append("--- TÀI LIỆU TÌM ĐƯỢC ---\n");
        for (SearchResultDTO chunk : finalContext) {
            rawContextBuilder.append(String.format("Nguồn: %s (Bài %s)\n", chunk.getSourceCitation(), chunk.getLessonId()));

            // Áp dụng Sliding Window để lấy context lân cận
            if (chunk.getMediaId() != null && chunk.getChunkIndex() != null && contextWindowSize > 0) {
                try {
                    List<String> neighbors = documentChunkRepository.findNeighboringChunks(
                            chunk.getMediaId(),
                            chunk.getChunkIndex(),
                            contextWindowSize
                    );
                    if (neighbors != null && !neighbors.isEmpty()) {
                        // Gộp nội dung các chunk lân cận lại theo thứ tự index
                        rawContextBuilder.append("Nội dung: ").append(String.join("\n", neighbors)).append("\n\n");
                    } else {
                        rawContextBuilder.append("Nội dung: ").append(chunk.getContent()).append("\n\n");
                    }
                } catch (Exception e) {
                    log.error("⚠️ Lỗi truy vấn neighboring chunks: {}", e.getMessage());
                    rawContextBuilder.append("Nội dung: ").append(chunk.getContent()).append("\n\n");
                }
            } else {
                rawContextBuilder.append("Nội dung: ").append(chunk.getContent()).append("\n\n");
            }
        }

        // 4. Lấy Citations để trả về cùng
        List<Citation> citations = finalContext.stream()
                .map(chunk -> Citation.builder()
                        .lessonId(chunk.getLessonId())
                        .contentType(chunk.getContentType())
                        .sourceCitation(chunk.getSourceCitation())
                        .build())
                .distinct()
                .toList();

        return ChatResponse.builder()
                .answer(rawContextBuilder.toString())
                .citations(citations)
                .build();
    }
    /**
     * Thuật toán Reciprocal Rank Fusion (RRF) để xếp hạng lại
     */
    private List<SearchResultDTO> performRRF(List<SearchResultDTO> vectorResults, List<SearchResultDTO> ftsResults, int limit) {
        Map<String, Double> rrfScores = new HashMap<>();
        Map<String, SearchResultDTO> documentMap = new HashMap<>();

        // Tính điểm RRF cho Vector Search
        calPoint(vectorResults, rrfScores, documentMap);

        // Tính điểm RRF cho Full-Text Search
        calPoint(ftsResults, rrfScores, documentMap);

        // Sắp xếp giảm dần theo điểm RRF và lấy tối đa limit
        return rrfScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> documentMap.get(entry.getKey()))
                .toList();
    }

    private void calPoint(List<SearchResultDTO> vectorResults, Map<String, Double> rrfScores, Map<String, SearchResultDTO> documentMap) {
        for (int i = 0; i < vectorResults.size(); i++) {
            SearchResultDTO doc = vectorResults.get(i);
            double rank = i + 1.0;
            rrfScores.put(doc.getId(), rrfScores.getOrDefault(doc.getId(), 0.0) + (1.0 / (rrfK + rank)));
            documentMap.putIfAbsent(doc.getId(), doc);
        }
    }

    /**
     * Map dữ liệu Object[] từ Native SQL sang SearchResultDto
     */
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
                    .contentType(row[6] != null ? ContentType.valueOf(row[6].toString().toUpperCase()) : null)
                    .sourceCitation((String) row[7])
                    .score(row[8] != null ? ((Number) row[8]).doubleValue() : 0.0)
                    .build());
        }
        return results;
    }


}
