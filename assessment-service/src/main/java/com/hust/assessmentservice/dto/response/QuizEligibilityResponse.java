package com.hust.assessmentservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizEligibilityResponse {
    private String quizId;
    private Integer maxAttempts;
    private Integer attemptsUsed;
    private Boolean hasPassed;
    private Double highestScore;
    private Boolean canAttemptAgain;
}
