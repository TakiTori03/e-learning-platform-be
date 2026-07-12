package com.hust.aiservice.config;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.hust.aiservice.repository.ChatMessageRepository;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class LangChain4jConfig {

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.chat-model:gemini-3.1-flash-lite}")
    private String flashModelName;

    @Value("${gemini.pro-model:gemini-2.5-flash}")
    private String proModelName;

    @Value("${gemini.temperature:0.2}")
    private Double temperature;

    @Bean
    public ChatMemoryStore chatMemoryStore(StringRedisTemplate redisTemplate, ChatMessageRepository messageRepository) {
        return new RedisChatMemoryStore(redisTemplate, 24, messageRepository); // 24 hours TTL
    }

    /**
     * Cung cấp ChatMemory tự động cho AiService, lưu tối đa 10 tin nhắn gần nhất sử dụng Redis để chia sẻ trạng thái và tự động hết hạn.
     */
    @Bean
    public ChatMemoryProvider chatMemoryProvider(ChatMemoryStore chatMemoryStore) {
        return memoryId -> MessageWindowChatMemory.builder()
                .chatMemoryStore(chatMemoryStore)
                .id(memoryId)
                .maxMessages(6)
                .build();
    }

    /**
     * Mô hình cấu hình thấp (Flash) dùng cho Worker Agents (nhanh, rẻ).
     */
    @Bean("flashChatModel")
    public ChatLanguageModel flashChatModel() {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(flashModelName)
                .temperature(temperature)
                .build();
    }

    @Bean("flashStreamingChatModel")
    public StreamingChatLanguageModel flashStreamingChatModel() {
        return GoogleAiGeminiStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName(flashModelName)
                .temperature(temperature)
                .build();
    }

    /**
     * Mô hình cấu hình cao (Pro) dùng cho Evaluator và Router (suy luận logic mạnh).
     */
    @Bean("proChatModel")
    public ChatLanguageModel proChatModel() {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(proModelName)
                .temperature(0.0) // Suy luận cần độ chính xác cao nhất
                .build();
    }

    @Value("${gemini.embedding-model:text-embedding-004}")
    private String embeddingModelName;

    /**
     * Khởi tạo Custom Embedding Model (do LangChain4j 0.36.2 chưa có sẵn class này cho API Key Google)
     */
    @Bean
    public EmbeddingModel geminiEmbeddingModel() {
        return new CustomGeminiEmbeddingModel(apiKey, embeddingModelName);
    }
}
