package com.hust.aiservice.config;

import com.hust.aiservice.agent.evaluator.EvaluatorAgent;
import com.hust.aiservice.agent.tools.TutorTools;
import com.hust.aiservice.agent.worker.GeneralAgent;
import com.hust.aiservice.agent.worker.TutorAgent;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình khởi tạo thủ công các AiService để đảm bảo mỗi Agent chỉ có quyền truy cập vào CÔNG CỤ (Tools) của riêng nó.
 */
@Configuration
public class MultiAgentConfig {

    @Bean
    public TutorAgent tutorAgent(@Qualifier("flashChatModel") ChatLanguageModel chatModel, @Qualifier("flashStreamingChatModel") StreamingChatLanguageModel streamingModel, ChatMemoryProvider chatMemoryProvider, TutorTools tutorTools) {
        return AiServices.builder(TutorAgent.class)
                .chatLanguageModel(chatModel)
                .streamingChatLanguageModel(streamingModel)
                .chatMemoryProvider(chatMemoryProvider)
                .tools(tutorTools) // Độc quyền Tool tài liệu
                .build();
    }

    @Bean
    public GeneralAgent generalAgent(@Qualifier("flashChatModel") ChatLanguageModel chatModel, @Qualifier("flashStreamingChatModel") StreamingChatLanguageModel streamingModel, ChatMemoryProvider chatMemoryProvider) {
        return AiServices.builder(GeneralAgent.class)
                .chatLanguageModel(chatModel)
                .streamingChatLanguageModel(streamingModel)
                .chatMemoryProvider(chatMemoryProvider)
                .build(); // Không dùng tool, chỉ chat chay
    }

    @Bean
    public EvaluatorAgent evaluatorAgent(@Qualifier("proChatModel") ChatLanguageModel chatModel) {
        return AiServices.builder(EvaluatorAgent.class)
                .chatLanguageModel(chatModel)
                .build(); // Đánh giá độc lập, không cần trí nhớ
    }
}
