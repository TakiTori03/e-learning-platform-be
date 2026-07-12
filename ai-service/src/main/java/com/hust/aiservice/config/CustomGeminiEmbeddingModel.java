package com.hust.aiservice.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Tự động implement giao thức EmbeddingModel của LangChain4j 
 * Do phiên bản 0.36.2 của module google-ai-gemini chưa tích hợp class này sẵn.
 */
@Slf4j
public class CustomGeminiEmbeddingModel implements EmbeddingModel {

    private final String apiKey;
    private final String modelName;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public CustomGeminiEmbeddingModel(String apiKey, String modelName) {
        this.apiKey = apiKey;
        this.modelName = modelName;
        this.objectMapper = new ObjectMapper();
        
        // Cấu hình Timeout
        org.springframework.http.client.SimpleClientHttpRequestFactory requestFactory = 
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(15000); 
        requestFactory.setReadTimeout(60000);    
        this.restTemplate = new RestTemplate(requestFactory);
    }

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
        if (textSegments == null || textSegments.isEmpty()) {
            return Response.from(new ArrayList<>());
        }

        List<Embedding> embeddings = new ArrayList<>();
        String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:batchEmbedContents?key=%s", modelName, apiKey);

        try {
            ObjectNode rootNode = objectMapper.createObjectNode();
            ArrayNode requestsArray = rootNode.putArray("requests");

            for (TextSegment segment : textSegments) {
                ObjectNode requestNode = requestsArray.addObject();
                requestNode.put("model", "models/" + modelName);
                ObjectNode contentNode = requestNode.putObject("content");
                contentNode.putArray("parts").addObject().put("text", segment.text());
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(rootNode.toString(), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            JsonNode responseJson = objectMapper.readTree(response.getBody());
            JsonNode embeddingsArray = responseJson.path("embeddings");

            if (embeddingsArray.isArray()) {
                for (JsonNode embNode : embeddingsArray) {
                    JsonNode valuesNode = embNode.path("values");
                    List<Float> vector = new ArrayList<>();
                    if (valuesNode.isArray()) {
                        for (JsonNode val : valuesNode) {
                            vector.add((float) val.asDouble());
                        }
                    }
                    
                    // Truncate to 768 dimensions if needed (pgvector)
                    if (vector.size() > 768) {
                        vector = vector.subList(0, 768);
                    }

                    embeddings.add(Embedding.from(vector));
                }
            }
        } catch (Exception e) {
            log.error("Lỗi khi sinh Batch Vector Embedding từ Gemini API: {}", e.getMessage());
            throw new RuntimeException("Batch Embedding API error: " + e.getMessage(), e);
        }

        return Response.from(embeddings);
    }
}
