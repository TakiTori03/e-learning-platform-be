package com.hust.assessmentservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizStartResponse {
    private String attemptId;
    private Instant startedAt;
    private Integer timeLimitMinutes;
    private List<StudentQuestionDTO> questions;
    private String quizTitle;
    private Double passingScorePercentage;
}
