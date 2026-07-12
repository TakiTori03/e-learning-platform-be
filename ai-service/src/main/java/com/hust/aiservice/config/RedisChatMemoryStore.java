package com.hust.aiservice.config;

import com.hust.aiservice.repository.ChatMessageRepository;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Distributed, self-expiring ChatMemoryStore backed by Redis, with PostgreSQL cold-restore fallback.
 * - Prevents JVM memory leaks (automatic TTL eviction).
 * - Enables seamless multi-instance horizontal scaling (shared memory).
 * - Automatically falls back to PostgreSQL on cache miss to prevent "memory loss" when Redis cache expires or restarts.
 */
@RequiredArgsConstructor
@Slf4j
public class RedisChatMemoryStore implements ChatMemoryStore {

    private final StringRedisTemplate redisTemplate;
    private final long ttlHours;
    private final ChatMessageRepository messageRepository;

    private String getRedisKey(Object memoryId) {
        return "elearning:ai-service:chat:memory:" + memoryId.toString();
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String key = getRedisKey(memoryId);
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null || json.isEmpty()) {
                log.info("💾 [Redis Cache Miss] Loading chat history from PostgreSQL for session: {}", memoryId);
                // Load the last 6 messages (matching the sliding window size of MessageWindowChatMemory - 3 pairs)
                List<com.hust.aiservice.entity.ChatMessage> dbMessages = messageRepository.findRecentMessages(
                        memoryId.toString(),
                        PageRequest.of(0, 6)
                );

                List<ChatMessage> langChainMessages = new ArrayList<>();
                if (dbMessages != null && !dbMessages.isEmpty()) {
                    // Sort chronologically (oldest to newest)
                    List<com.hust.aiservice.entity.ChatMessage> sortedDbMessages = dbMessages.stream()
                            .sorted((m1, m2) -> m1.getCreatedAt().compareTo(m2.getCreatedAt()))
                            .toList();

                    for (com.hust.aiservice.entity.ChatMessage dbMsg : sortedDbMessages) {
                        if ("user".equalsIgnoreCase(dbMsg.getRole())) {
                            langChainMessages.add(dev.langchain4j.data.message.UserMessage.from(dbMsg.getContent()));
                        } else if ("assistant".equalsIgnoreCase(dbMsg.getRole())) {
                            langChainMessages.add(dev.langchain4j.data.message.AiMessage.from(dbMsg.getContent()));
                        } else if ("system".equalsIgnoreCase(dbMsg.getRole())) {
                            langChainMessages.add(dev.langchain4j.data.message.SystemMessage.from(dbMsg.getContent()));
                        }
                    }

                    // Hydrate Redis cache with these messages
                    updateMessages(memoryId, langChainMessages);
                }
                return langChainMessages;
            }
            // Sliding window TTL: refresh expiration time on read
            redisTemplate.expire(key, ttlHours, TimeUnit.HOURS);
            return ChatMessageDeserializer.messagesFromJson(json);
        } catch (Exception e) {
            log.error("⚠️ Error reading ChatMemory from Redis/DB for session {}: {}", memoryId, e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String key = getRedisKey(memoryId);
        try {
            if (messages == null || messages.isEmpty()) {
                redisTemplate.delete(key);
                return;
            }
            String json = ChatMessageSerializer.messagesToJson(messages);
            redisTemplate.opsForValue().set(key, json, ttlHours, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("⚠️ Error writing ChatMemory to Redis for session {}: {}", memoryId, e.getMessage());
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {
        String key = getRedisKey(memoryId);
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("⚠️ Error deleting ChatMemory from Redis for session {}: {}", memoryId, e.getMessage());
        }
    }
}
