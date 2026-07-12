package com.hust.assessmentservice.dto.response;

import com.hust.assessmentservice.entity.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponse {
    private String id;
    private String questionText;
    private QuestionType type;
    private List<AnswerOptionResponse> options;
    private Double scoreWeight;
}
