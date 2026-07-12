package com.hust.aiservice.dto.response;

import com.hust.aiservice.dto.Citation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String sessionId;
    private String answer;
    private List<Citation> citations;

    /** Tên Agent đã xử lý câu hỏi (TUTOR_AGENT, ADVISOR_AGENT, ...) */
    private String routedAgent;

    /** Điểm đánh giá chất lượng câu trả lời từ Evaluator (0.0 - 1.0) */
    private Double evaluationScore;
}
