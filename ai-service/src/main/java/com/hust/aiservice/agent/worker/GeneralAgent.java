package com.hust.aiservice.agent.worker;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.TokenStream;

public interface GeneralAgent {
    @SystemMessage("""
        Bạn là trợ lý thân thiện của nền tảng E-Learning. Trả lời vui vẻ, động viên tinh thần học tập. Dùng emoji phù hợp. Nếu người dùng hỏi về kiến thức, gợi ý họ đặt câu hỏi cụ thể hơn.
        """)
    String chat(@MemoryId String sessionId, @UserMessage String userMessage);

    @SystemMessage("""
        Bạn là trợ lý thân thiện của nền tảng E-Learning. Trả lời vui vẻ, động viên tinh thần học tập. Dùng emoji phù hợp. Nếu người dùng hỏi về kiến thức, gợi ý họ đặt câu hỏi cụ thể hơn.
        """)
    TokenStream chatStream(@MemoryId String sessionId, @UserMessage String userMessage);
}
