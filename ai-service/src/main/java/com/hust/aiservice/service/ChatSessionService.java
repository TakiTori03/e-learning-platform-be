package com.hust.aiservice.service;

import com.hust.aiservice.entity.ChatMessage;
import com.hust.aiservice.entity.ChatSession;



public interface ChatSessionService {
    ChatSession getOrCreateSession(String sessionId, String userId, String courseId, String firstMessage);
    ChatMessage saveMessage4(String sessionId, String role, String content, String citationsJson);
    ChatMessage saveMessage(String sessionId, String role, String content, String citationsJson, String routedAgent);
    java.util.List<ChatSession> getSessions(String userId, String courseId);
    java.util.List<ChatMessage> getMessages(String sessionId, String userId);
    void deleteSession(String sessionId, String userId);
}
