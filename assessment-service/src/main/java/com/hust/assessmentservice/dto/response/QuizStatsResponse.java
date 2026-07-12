package com.hust.assessmentservice.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizStatsResponse {
    private long totalQuizzes;
    private long linkedQuizzes;
    private long unlinkedQuizzes;
    private double avgTimeLimit;
}
