package com.hust.aiservice.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hust.aiservice.dto.SearchResultDTO;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class CohereRerankService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Getter
    @Value("${cohere.rerank.enabled:false}")
    private boolean enabled;

    @Value("${cohere.rerank.api-key:}")
    private String apiKey;

    @Value("${cohere.rerank.url:https://api.cohere.com/v1/rerank}")
    private String url;

    @Value("${cohere.rerank.model:rerank-multilingual-v3.0}")
    private String model;

    public CohereRerankService(
            ObjectMapper objectMapper,
            @Value("${cohere.rerank.connect-timeout-ms:10000}") int connectTimeoutMs,
            @Value("${cohere.rerank.read-timeout-ms:20000}") int readTimeoutMs) {
        this.objectMapper = objectMapper;
        
        org.springframework.http.client.SimpleClientHttpRequestFactory requestFactory = 
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);
        this.restTemplate = new RestTemplate(requestFactory);
    }
    public List<SearchResultDTO> rerank(String query, List<SearchResultDTO> candidates, int topK) {
        if (candidates == null) {
            return java.util.Collections.emptyList();
        }
        if (!enabled || apiKey == null || apiKey.trim().isEmpty() || apiKey.contains("your_cohere_key_here") || candidates.isEmpty()) {
            log.info("ℹ️ Cohere Rerank is disabled or API key not configured, returning original order.");
            return candidates.subList(0, Math.min(candidates.size(), topK));
        }

        log.info("🎯 Bắt đầu gọi Cohere Rerank cho {} ứng viên...", candidates.size());
        try {
            // Build request body
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);
            requestBody.put("query", query);
            requestBody.put("top_n", topK);

            ArrayNode documentsNode = requestBody.putArray("documents");
            for (SearchResultDTO doc : candidates) {
                documentsNode.add(doc.getContent());
            }

            // Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey.trim());

            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            JsonNode responseJson = objectMapper.readTree(response.getBody());
            JsonNode resultsNode = responseJson.path("results");

            List<SearchResultDTO> reranked = new ArrayList<>();
            if (resultsNode.isArray()) {
                for (JsonNode result : resultsNode) {
                    int originalIndex = result.path("index").asInt();
                    double relevanceScore = result.path("relevance_score").asDouble();
                    
                    if (originalIndex >= 0 && originalIndex < candidates.size()) {
                        SearchResultDTO candidate = candidates.get(originalIndex);
                        // Update score with relevance score from Cohere
                        candidate.setScore(relevanceScore);
                        reranked.add(candidate);
                    }
                }
            }

            log.info("✅ Cohere Rerank hoàn tất. Lọc từ {} xuống {} chunks.", candidates.size(), reranked.size());
            return reranked;
        } catch (Exception e) {
            log.error("❌ Lỗi xảy ra khi gọi Cohere Rerank API: {}", e.getMessage(), e);
            // Fallback to topK of original list in case of error
            return candidates.subList(0, Math.min(candidates.size(), topK));
        }
    }
}
