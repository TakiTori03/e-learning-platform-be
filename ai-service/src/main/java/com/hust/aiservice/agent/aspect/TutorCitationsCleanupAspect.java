package com.hust.aiservice.agent.aspect;

import com.hust.aiservice.agent.tools.TutorTools;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class TutorCitationsCleanupAspect {

    @After("execution(* com.hust.aiservice.controller.TutorChatController.*(..)) || " +
           "execution(* com.hust.aiservice.controller.GlobalChatController.*(..)) || " +
           "execution(* com.hust.aiservice.agent.MultiAgentOrchestrator.*(..))")
    public void cleanupCitations() {
        TutorTools.clear();
        log.debug("🧼 [AOP Citation] Tự động giải phóng ThreadLocal Tutor Citations.");
    }
}
