package com.hust.aiservice.agent.worker.impl;

import com.hust.aiservice.agent.enums.AgentType;
import com.hust.aiservice.agent.worker.AgentWorker;
import com.hust.aiservice.agent.worker.TutorAgent;
import dev.langchain4j.service.TokenStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TutorWorker implements AgentWorker {
    private final TutorAgent tutorAgent;

    @Override
    public AgentType getType() {
        return AgentType.TUTOR;
    }

    @Override
    public String chat(String sessionId, String courseId, String message) {
        return tutorAgent.chat(sessionId, courseId, message);
    }

    @Override
    public TokenStream chatStream(String sessionId, String courseId, String message) {
        return tutorAgent.chatStream(sessionId, courseId, message);
    }
}
