package com.hust.aiservice.agent.worker;

import com.hust.aiservice.agent.enums.AgentType;
import dev.langchain4j.service.TokenStream;

public interface AgentWorker {
    AgentType getType();
    String chat(String sessionId, String courseId, String message);
    TokenStream chatStream(String sessionId, String courseId, String message);
}
