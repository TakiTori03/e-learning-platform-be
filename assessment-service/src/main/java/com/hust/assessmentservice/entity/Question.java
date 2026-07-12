package com.hust.assessmentservice.entity;

import com.hust.assessmentservice.entity.enums.QuestionType;
import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Question {
    private String id; // UUID sinh tự động cho câu hỏi
    private String questionText;
    private QuestionType type; // SINGLE_CHOICE, MULTI_CHOICE
    private List<AnswerOption> options;
    private Double scoreWeight; // Trọng số điểm của câu hỏi (mặc định: 1.0)
}
