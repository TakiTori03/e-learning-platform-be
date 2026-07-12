package com.hust.aiservice.agent;

import com.hust.aiservice.agent.enums.AgentType;
import com.hust.aiservice.agent.enums.EvaluationDecision;
import com.hust.aiservice.agent.evaluator.EvaluatorAgent;
import com.hust.aiservice.agent.tools.SearchTools;
import com.hust.aiservice.agent.worker.GeneralAgent;
import com.hust.aiservice.agent.worker.TutorAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import dev.langchain4j.service.TokenStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class MultiAgentOrchestrator {

    private final TutorAgent tutorAgent;
    private final GeneralAgent generalAgent;
    private final EvaluatorAgent evaluatorAgent;
    private final SearchTools searchTools;
    private final com.hust.aiservice.repository.AgentRoutingLogRepository agentRoutingLogRepository;

    public com.hust.aiservice.dto.response.OrchestratorResult orchestrateTutor(String sessionId, String courseId, String originalMessage) {
        log.info("🎯 [Orchestrator] Bắt đầu xử lý luồng TUTOR RAG cho session: {}", sessionId);
        long startTime = System.currentTimeMillis();

        String currentQuery = originalMessage;
        int loopCount = 0;
        int maxLoops = 2;
        double evaluationScore = 1.0;
        boolean wasCorrected = false;

        String answer = "";
        while (loopCount < maxLoops) {
            loopCount++;

            // 1. Chạy trực tiếp Tutor Agent (RAG)
            answer = tutorAgent.chat(sessionId, courseId, currentQuery);
            log.info("🤖 [Tutor Agent RAG] Trả về câu trả lời");

            // 2. EVALUATOR: Chấm điểm
            String evaluationPrompt = String.format("Câu hỏi: %s\nCâu trả lời: %s", currentQuery, answer);
            EvaluationDecision decision = evaluatorAgent.evaluate(evaluationPrompt);
            log.info("⚖️ [Evaluator] Đánh giá: {}", decision);

            if (decision == EvaluationDecision.PASS) {
                evaluationScore = 1.0;
                break;
            }

            // 3. SELF-CORRECTION
            wasCorrected = true;
            evaluationScore = 0.0;
            log.warn("🔄 [Self-Correction] Thử lại bằng Web Search...");
            String webContext = searchTools.searchWeb(originalMessage);
            currentQuery = String.format(
                "Người dùng hỏi: '%s'. Hãy dùng thông tin Web sau để trả lời: %s", 
                originalMessage, webContext
            );
        }

        if (loopCount >= maxLoops && wasCorrected && evaluationScore == 0.0) {
            answer = generalAgent.chat(sessionId, 
                "Tôi đã cố gắng tìm kiếm tài liệu và Internet nhưng chưa có câu trả lời tốt cho bạn về: " + originalMessage);
        }

        long latencyMs = System.currentTimeMillis() - startTime;

        // Lưu log định tuyến
        try {
            com.hust.aiservice.entity.AgentRoutingLog routingLog = com.hust.aiservice.entity.AgentRoutingLog.builder()
                    .sessionId(sessionId)
                    .userQuery(originalMessage)
                    .routedAgent(AgentType.TUTOR.name())
                    .confidenceScore(1.0)
                    .evaluationScore(evaluationScore)
                    .wasCorrected(wasCorrected)
                    .correctionSource(wasCorrected ? "WEB_SEARCH" : null)
                    .latencyMs(latencyMs)
                    .createdAt(java.time.LocalDateTime.now())
                    .build();
            agentRoutingLogRepository.save(routingLog);
        } catch (Exception e) {
            log.error("⚠️ Không thể lưu log định tuyến: {}", e.getMessage());
        }

        return com.hust.aiservice.dto.response.OrchestratorResult.builder()
                .answer(answer)
                .agentType(AgentType.TUTOR)
                .evaluationScore(evaluationScore)
                .wasCorrected(wasCorrected)
                .build();
    }

    public TokenStream streamOrchestrateTutor(String sessionId, String courseId, String originalMessage) {
        log.info("🎯 [Orchestrator] Bắt đầu luồng TUTOR RAG STREAMING cho session: {}", sessionId);
        return tutorAgent.chatStream(sessionId, courseId, originalMessage);
    }
}
