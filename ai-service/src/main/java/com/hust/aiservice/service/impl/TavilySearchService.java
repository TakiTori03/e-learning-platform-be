package com.hust.aiservice.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hust.aiservice.dto.response.WebSearchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tavily Search Service — Tìm kiếm Internet cho Corrective RAG.
 *
 * Được kích hoạt khi SelfCorrectionEvaluator đánh giá câu trả lời từ RAG
 * không đạt chất lượng (suggestion = WEB_SEARCH). Tavily API được thiết kế
 * tối ưu riêng cho LLM/RAG, trả về kết quả sạch không chứa HTML thừa.
 *
 * API: https://api.tavily.com/search
 */
@Service
@Slf4j
public class TavilySearchService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${tavily.enabled:false}")
    private boolean enabled;

    @Value("${tavily.api-key:}")
    private String apiKey;

    @Value("${tavily.base-url:https://api.tavily.com}")
    private String baseUrl;

    @Value("${tavily.max-results:3}")
    private int maxResults;

    @Value("${tavily.search-depth:advanced}")
    private String searchDepth;

    public TavilySearchService(
            ObjectMapper objectMapper,
            @Value("${tavily.connect-timeout-ms:10000}") int connectTimeoutMs,
            @Value("${tavily.read-timeout-ms:15000}") int readTimeoutMs) {
        this.objectMapper = objectMapper;

        org.springframework.http.client.SimpleClientHttpRequestFactory requestFactory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);
        this.restTemplate = new RestTemplate(requestFactory);
    }

    /**
     * Kiểm tra Tavily đã được cấu hình API key chưa.
     */
    public boolean isEnabled() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    /**
     * Tìm kiếm Internet qua Tavily API.
     *
     * @param query Câu hỏi cần tìm kiếm
     * @return Danh sách kết quả tìm kiếm web (title, content, url, score)
     */
    public List<WebSearchResponse> search(String query) {
        if (!isEnabled()) {
            log.warn("⚠️ Tavily API key chưa được cấu hình. Bỏ qua Web Search.");
            return Collections.emptyList();
        }

        log.info("🌐 Tavily đang tìm kiếm: '{}'", query);

        try {
            String url = baseUrl + "/search";

            // Build request body
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("api_key", apiKey);
            requestBody.put("query", query);
            requestBody.put("max_results", maxResults);
            requestBody.put("search_depth", searchDepth);
            requestBody.put("include_answer", false);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            JsonNode responseJson = objectMapper.readTree(response.getBody());
            JsonNode resultsNode = responseJson.path("results");

            List<WebSearchResponse> results = new ArrayList<>();
            if (resultsNode.isArray()) {
                for (JsonNode resultNode : resultsNode) {
                    results.add(WebSearchResponse.builder()
                            .title(resultNode.path("title").asText(""))
                            .content(resultNode.path("content").asText(""))
                            .url(resultNode.path("url").asText(""))
                            .score(resultNode.path("score").asDouble(0.0))
                            .build());
                }
            }

            log.info("🌐 Tavily trả về {} kết quả.", results.size());
            return results;
        } catch (Exception e) {
            log.error("❌ Lỗi khi gọi Tavily Search API: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Ghép danh sách kết quả web thành chuỗi context để gửi cho LLM tổng hợp.
     */
    public String formatAsContext(List<WebSearchResponse> results) {
        if (results == null || results.isEmpty()) {
            return "Không tìm thấy kết quả từ Internet.";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            WebSearchResponse r = results.get(i);
            sb.append(String.format("[Web %d] %s\nNguồn: %s\nNội dung: %s\n\n",
                    i + 1, r.getTitle(), r.getUrl(), r.getContent()));
        }
        return sb.toString();
    }
}
