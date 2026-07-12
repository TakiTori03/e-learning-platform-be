package com.hust.assessmentservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.hust.commonlibrary.entity.QuizType;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizResponse {
    private String id;
    private String courseId;
    private String lessonId;
    private String authorId;
    private String title;
    private String description;
    private Integer timeLimitMinutes;
    private Double passingScorePercentage;
    private Integer maxAttempts;
    private QuizType type;
    private List<QuestionResponse> questions;
    private Instant createdAt;
    private Instant updatedAt;
}
