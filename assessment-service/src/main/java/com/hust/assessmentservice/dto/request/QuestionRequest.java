package com.hust.assessmentservice.dto.request;

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
public class QuestionRequest {
    private String questionText;
    private QuestionType type;
    private List<AnswerOptionRequest> options;
    private Double scoreWeight;
}
