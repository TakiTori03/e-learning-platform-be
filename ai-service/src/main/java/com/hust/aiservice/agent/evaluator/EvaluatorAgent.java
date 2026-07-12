package com.hust.aiservice.agent.evaluator;

import com.hust.aiservice.agent.enums.EvaluationDecision;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface EvaluatorAgent {

    @SystemMessage("""
        Bạn là một Giám khảo đánh giá (Evaluator Agent).
        Nhiệm vụ: Đọc câu hỏi của người dùng và câu trả lời dự kiến của Agent.
        
        Luật đánh giá:
        1. Nếu câu trả lời có chứa thông tin thực tế, mạch lạc, giải quyết được câu hỏi -> Trả về PASS
        2. Nếu câu trả lời nói rằng "Không tìm thấy thông tin", "Tôi không biết", hoặc thiếu logic, cần phải tra cứu Google -> Trả về FAIL_NEEDS_SEARCH
        
        KHÔNG GIẢI THÍCH. CHỈ TRẢ VỀ: PASS hoặc FAIL_NEEDS_SEARCH.
        """)
    EvaluationDecision evaluate(@UserMessage String prompt);
}
