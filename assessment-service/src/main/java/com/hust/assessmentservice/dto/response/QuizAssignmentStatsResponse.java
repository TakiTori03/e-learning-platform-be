package com.hust.assessmentservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizAssignmentStatsResponse {
    private long totalCount;
    private long completedCount;
    private long inProgressCount;
    private double completionRate;
}
