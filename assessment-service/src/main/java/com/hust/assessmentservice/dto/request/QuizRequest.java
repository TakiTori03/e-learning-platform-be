package com.hust.assessmentservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.hust.commonlibrary.entity.QuizType;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizRequest {
    private String courseId;
    private String lessonId;
    private String authorId;
    private String title;
    private String description;
    private Integer timeLimitMinutes;
    private Double passingScorePercentage;
    private Integer maxAttempts;
    private QuizType type;
    private List<QuestionRequest> questions;
}
