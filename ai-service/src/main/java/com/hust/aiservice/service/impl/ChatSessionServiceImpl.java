package com.hust.aiservice.service.impl;

import com.hust.aiservice.entity.ChatMessage;
import com.hust.aiservice.entity.ChatSession;
import com.hust.aiservice.repository.ChatMessageRepository;
import com.hust.aiservice.repository.ChatSessionRepository;
import com.hust.aiservice.service.ChatSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatSessionServiceImpl implements ChatSessionService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @Value("${rag.max-history-pairs:3}")
    private int maxHistoryPairs;

    /** Tạo phiên mới hoặc tải phiên cũ */
    @Transactional
    public ChatSession getOrCreateSession(String sessionId, String userId, String courseId, String firstMessage) {
        if (sessionId != null && !sessionId.trim().isEmpty()) {
            return sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ChatSession với ID: " + sessionId));
        }

        // Tạo phiên mới, title = 50 ký tự đầu của câu hỏi
        String title = firstMessage;
        if (title != null && title.length() > 50) {
            title = title.substring(0, 50) + "...";
        }

        ChatSession newSession = ChatSession.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .courseId(courseId)
                .title(title)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return sessionRepository.save(newSession);
    }

    /** Lấy tối đa 6 tin nhắn gần nhất (3 cặp hỏi-đáp) */
    public List<ChatMessage> getRecentHistory(String sessionId) {
        List<ChatMessage> recent = messageRepository.findRecentMessages(
            sessionId, PageRequest.of(0, maxHistoryPairs * 2)
        );
        // Đảo ngược để đúng thứ tự thời gian
        return recent.stream().sorted((m1, m2) -> m1.getCreatedAt().compareTo(m2.getCreatedAt())).toList();
    }

    /** Lưu tin nhắn mới vào DB */
    @Transactional
    public ChatMessage saveMessage4(String sessionId, String role, String content, String citationsJson) {
        return saveMessage(sessionId, role, content, citationsJson, null);
    }

    /** Lưu tin nhắn mới vào DB (kèm thông tin Agent đã xử lý) */
    @Transactional
    public ChatMessage saveMessage(String sessionId, String role, String content,
                                    String citationsJson, String routedAgent) {
        ChatMessage message = ChatMessage.builder()
                .sessionId(sessionId)
                .role(role)
                .content(content)
                .citations(citationsJson)
                .routedAgent(routedAgent)
                .createdAt(LocalDateTime.now())
                .build();
        
        ChatMessage saved = messageRepository.save(message);
        
        // Cập nhật thời gian update cho session
        sessionRepository.findById(sessionId).ifPresent(session -> {
            session.setUpdatedAt(LocalDateTime.now());
            sessionRepository.save(session);
        });

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatSession> getSessions(String userId, String courseId) {
        return sessionRepository.getSessions(userId, courseId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessage> getMessages(String sessionId, String userId) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ChatSession với ID: " + sessionId));
        if (!session.getUserId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền truy cập hội thoại này.");
        }
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    @Override
    @Transactional
    public void deleteSession(String sessionId, String userId) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ChatSession với ID: " + sessionId));
        if (!session.getUserId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền xóa hội thoại này.");
        }
        messageRepository.deleteBySessionId(sessionId);
        sessionRepository.delete(session);
        try {
            String key = "elearning:ai-service:chat:memory:" + sessionId;
            redisTemplate.delete(key);
            log.info("🧹 Đã xóa bộ nhớ chat (Redis memory) của session: {}", sessionId);
        } catch (Exception e) {
            log.error("❌ Không thể xóa Redis memory cho session {}: {}", sessionId, e.getMessage());
        }
    }
}
