package com.hust.aiservice.agent.worker.impl;

import com.hust.aiservice.agent.enums.AgentType;
import com.hust.aiservice.agent.worker.AgentWorker;
import com.hust.aiservice.agent.worker.GeneralAgent;
import dev.langchain4j.service.TokenStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GeneralWorker implements AgentWorker {
    private final GeneralAgent generalAgent;

    @Override
    public AgentType getType() {
        return AgentType.GENERAL;
    }

    @Override
    public String chat(String sessionId, String courseId, String message) {
        return generalAgent.chat(sessionId, message);
    }

    @Override
    public TokenStream chatStream(String sessionId, String courseId, String message) {
        return generalAgent.chatStream(sessionId, message);
    }
}
