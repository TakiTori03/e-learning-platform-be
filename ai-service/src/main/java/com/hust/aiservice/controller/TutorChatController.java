package com.hust.aiservice.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hust.aiservice.agent.MultiAgentOrchestrator;
import com.hust.aiservice.agent.enums.AgentType;
import com.hust.aiservice.dto.request.ChatRequest;
import com.hust.aiservice.dto.response.ChatResponse;
import com.hust.aiservice.entity.ChatSession;
import com.hust.aiservice.service.ChatSessionService;
import com.hust.aiservice.service.SemanticCacheService;
import com.hust.commonlibrary.annotation.RateLimit;
import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.commonlibrary.utils.SecurityUtils;

import dev.langchain4j.service.TokenStream;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/chat/tutor")
@RequiredArgsConstructor
@Slf4j
public class TutorChatController {

    private final MultiAgentOrchestrator agentOrchestrator;
    private final ChatSessionService chatSessionService;
    private final ObjectMapper objectMapper;
    private final SemanticCacheService semanticCacheService;
    private final com.hust.aiservice.service.PromptGuardService promptGuardService;

    @PostMapping
    @RateLimit(limit = 15, period = 60)
    public ResponseEntity<ApiResponse<ChatResponse>> tutorChat(@RequestBody @Valid ChatRequest request) {
        log.info("📩 [Tutor] Nhận yêu cầu chat cho Course [{}]: {}", request.getCourseId(), request.getMessage());

        if (!promptGuardService.isSafePrompt(request.getMessage())) {
            return ResponseEntity.badRequest().body(
                ApiResponse.<ChatResponse>builder()
                    .success(false)
                    .message("Yêu cầu của bạn vi phạm chính sách an toàn của hệ thống (Nghi ngờ Prompt Injection).")
                    .build()
            );
        }

        String userId = SecurityUtils.getCurrentUserIdOrThrow();

        if (request.getSessionId() == null && (request.getCourseId() == null || request.getCourseId().isBlank())) {
            return ResponseEntity.badRequest().body(
                ApiResponse.<ChatResponse>builder()
                    .success(false)
                    .message("Yêu cầu chat Tutor mới bắt buộc phải cung cấp courseId.")
                    .build()
            );
        }

        ChatSession session = chatSessionService.getOrCreateSession(
                request.getSessionId(),
                userId,
                request.getCourseId(),
                request.getMessage()
        );

        String sessionId = session.getId();
        String courseId = request.getCourseId();
        if ((courseId == null || courseId.isBlank()) && session.getCourseId() != null) {
            courseId = session.getCourseId();
        }

        ChatResponse cachedResponse = semanticCacheService.getCachedResponse(courseId, request.getMessage());
        if (cachedResponse != null) {
            log.info("🚀 [Cache Hit] Trả về câu trả lời đã cache cho session: {}", sessionId);
            saveMessageUser(request, sessionId, cachedResponse);
            cachedResponse.setSessionId(sessionId);
            return ResponseEntity.ok(ApiResponse.<ChatResponse>builder()
                    .success(true)
                    .message("Success")
                    .payload(cachedResponse)
                    .build());
        }

        com.hust.aiservice.dto.response.OrchestratorResult result;
        boolean isError = false;
        try {
            result = agentOrchestrator.orchestrateTutor(
                    sessionId,
                    courseId,
                    request.getMessage()
            );
        } catch (Exception e) {
            log.error("Lỗi khi gọi Tutor Agent: ", e);
            result = com.hust.aiservice.dto.response.OrchestratorResult.builder()
                    .answer("⚠️ Hệ thống AI hiện đang quá tải hoặc gặp sự cố kỹ thuật. Vui lòng thử lại sau ít phút.")
                    .agentType(AgentType.TUTOR)
                    .evaluationScore(1.0)
                    .wasCorrected(false)
                    .build();
            isError = true;
        }

        ChatResponse response = ChatResponse.builder()
                .answer(result.getAnswer())
                .citations(result.getCitations() != null ? result.getCitations() : java.util.Collections.emptyList())
                .routedAgent(result.getAgentType().name())
                .evaluationScore(result.getEvaluationScore())
                .build();

        if (!isError) {
            semanticCacheService.saveToCache(courseId, request.getMessage(), response);

            chatSessionService.saveMessage4(sessionId, "user", request.getMessage(), null);

            String citationsJson = null;
            try {
                citationsJson = objectMapper.writeValueAsString(response.getCitations());
            } catch (Exception e) {
                log.error("⚠️ Không thể serialize citations: {}", e.getMessage());
            }

            chatSessionService.saveMessage(
                    sessionId, "assistant", response.getAnswer(), citationsJson, response.getRoutedAgent());
        }

        response.setSessionId(sessionId);

        return ResponseEntity.ok(ApiResponse.<ChatResponse>builder()
                .success(true)
                .message("Success")
                .payload(response)
                .build());
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RateLimit(limit = 15, period = 60)
    public SseEmitter tutorChatStream(@RequestBody @Valid ChatRequest request) {
        log.info("📩 Nhận yêu cầu stream chat TUTOR RAG cho Course [{}]: {}", request.getCourseId(), request.getMessage());

        SseEmitter emitter = new SseEmitter(0L);

        if (!promptGuardService.isSafePrompt(request.getMessage())) {
            try {
                emitter.send(SseEmitter.event().name("message").data("\n\n🚨 Câu hỏi của bạn vi phạm chính sách sử dụng (Dấu hiệu thao túng hệ thống). Vui lòng hỏi lại!"));
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
            } catch (Exception e) {}
            return emitter;
        }

        String userId = SecurityUtils.getCurrentUserIdOrThrow();

        if (request.getSessionId() == null && (request.getCourseId() == null || request.getCourseId().isBlank())) {
            try {
                emitter.send(SseEmitter.event().name("message").data("\n\n🚨 Yêu cầu chat Tutor mới bắt buộc phải cung cấp courseId."));
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
            } catch (Exception e) {}
            return emitter;
        }

        ChatSession session = chatSessionService.getOrCreateSession(
                request.getSessionId(),
                userId,
                request.getCourseId(),
                request.getMessage()
        );

        String sessionId = session.getId();
        String tempCourseId = request.getCourseId();
        if ((tempCourseId == null || tempCourseId.isBlank()) && session.getCourseId() != null) {
            tempCourseId = session.getCourseId();
        }
        final String courseId = tempCourseId;

        ChatResponse cachedResponse = semanticCacheService.getCachedResponse(courseId, request.getMessage());
        if (cachedResponse != null) {
            log.info("🚀 [Stream Cache Hit] Trả về dữ liệu từ cache cho session: {}", sessionId);
            saveMessageUser(request, sessionId, cachedResponse);

            String metadataJson = "{}";
            try {
                java.util.Map<String, Object> metaMap = new java.util.HashMap<>();
                metaMap.put("sessionId", sessionId);
                metaMap.put("citations", cachedResponse.getCitations());
                metadataJson = objectMapper.writeValueAsString(metaMap);
            } catch (Exception e) {
                log.error("⚠️ Lỗi serialize metadata: {}", e.getMessage());
            }

            try {
                emitter.send(SseEmitter.event().name("metadata").data(metadataJson));
                emitter.send(SseEmitter.event().name("message").data(cachedResponse.getAnswer()));
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
            } catch (Exception e) {
                log.error("⚠️ Lỗi gửi cache event: {}", e.getMessage());
                emitter.completeWithError(e);
            }

            return emitter;
        }

        TokenStream tokenStream = agentOrchestrator.streamOrchestrateTutor(sessionId, courseId, request.getMessage());

        String metadataJson = "{}";
        try {
            java.util.Map<String, Object> metaMap = new java.util.HashMap<>();
            metaMap.put("sessionId", sessionId);
            metaMap.put("routedAgent", AgentType.TUTOR.name());
            metadataJson = objectMapper.writeValueAsString(metaMap);
        } catch (Exception e) {
            log.error("⚠️ Lỗi serialize metadata: {}", e.getMessage());
        }

        try {
            emitter.send(SseEmitter.event().name("metadata").data(metadataJson));
        } catch (Exception e) {
            log.error("⚠️ Lỗi gửi metadata: {}", e.getMessage());
        }

        tokenStream
            .onNext(token -> {
                try {
                    emitter.send(SseEmitter.event().name("message").data(token));
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            })
            .onComplete(response -> {
                chatSessionService.saveMessage4(sessionId, "user", request.getMessage(), null);

                java.util.List<com.hust.aiservice.dto.Citation> citations = com.hust.aiservice.agent.tools.TutorTools.getCitations();
                String citationsJson = null;
                if (citations != null && !citations.isEmpty()) {
                    try {
                        citationsJson = objectMapper.writeValueAsString(citations);
                    } catch (Exception e) {
                        log.error("⚠️ Không thể serialize citations: {}", e.getMessage());
                    }
                }

                chatSessionService.saveMessage(sessionId, "assistant", response.content().text(), citationsJson, AgentType.TUTOR.name());

                ChatResponse cacheResponse = ChatResponse.builder()
                        .answer(response.content().text())
                        .citations(citations != null ? citations : java.util.Collections.emptyList())
                        .routedAgent(AgentType.TUTOR.name())
                        .build();
                semanticCacheService.saveToCache(courseId, request.getMessage(), cacheResponse);

                com.hust.aiservice.agent.tools.TutorTools.clear();

                try {
                    emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                    emitter.complete();
                } catch (Exception e) {
                    log.error("⚠️ Lỗi gửi done event: {}", e.getMessage());
                }
            })
            .onError(error -> {
                log.error("Lỗi trong quá trình stream: ", error);
                try {
                    emitter.send(SseEmitter.event().name("message").data("\n\n⚠️ Hệ thống AI đang quá tải hoặc gặp sự cố, vui lòng thử lại sau ít phút."));
                    emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                    emitter.complete();
                } catch (Exception e) {
                    emitter.completeWithError(error);
                }
            })
            .start();

        return emitter;
    }

    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<List<ChatSession>>> getSessions(
            @RequestParam("courseId") String courseId) {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        List<ChatSession> sessions = chatSessionService.getSessions(userId, courseId);
        return ResponseEntity.ok(ApiResponse.<List<ChatSession>>builder()
                .success(true)
                .message("Success")
                .payload(sessions)
                .build());
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<ApiResponse<List<com.hust.aiservice.entity.ChatMessage>>> getMessages(
            @PathVariable("sessionId") String sessionId) {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        List<com.hust.aiservice.entity.ChatMessage> messages = chatSessionService.getMessages(sessionId, userId);
        return ResponseEntity.ok(ApiResponse.<List<com.hust.aiservice.entity.ChatMessage>>builder()
                .success(true)
                .message("Success")
                .payload(messages)
                .build());
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> deleteSession(
            @PathVariable("sessionId") String sessionId) {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        chatSessionService.deleteSession(sessionId, userId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Hội thoại đã được xóa thành công.")
                .build());
    }

    private void saveMessageUser(ChatRequest request, String sessionId, ChatResponse cachedResponse) {
        chatSessionService.saveMessage4(sessionId, "user", request.getMessage(), null);
        String citationsJson = null;
        try {
            citationsJson = objectMapper.writeValueAsString(cachedResponse.getCitations());
        } catch (Exception e) {
            log.error("⚠️ Lỗi serialize citations: {}", e.getMessage());
        }
        chatSessionService.saveMessage4(sessionId, "assistant", cachedResponse.getAnswer(), citationsJson);
    }
}
