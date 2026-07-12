package com.hust.aiservice.service;

import com.hust.aiservice.dto.response.ChatResponse;

public interface RagService {
    ChatResponse retrieveContext(String courseId, String query);
}
