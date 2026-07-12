package com.hust.aiservice.service;

import com.hust.aiservice.dto.response.ChatResponse;

public interface SemanticCacheService {
    ChatResponse getCachedResponse(String courseId, String query);
    void saveToCache(String courseId, String query, ChatResponse response);
    void clearCache(String courseId);
}
