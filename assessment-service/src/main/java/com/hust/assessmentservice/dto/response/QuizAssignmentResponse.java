package com.hust.assessmentservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizAssignmentResponse {
    private String id;
    private String courseId;
    private String lessonId;
    private String title;
    private String description;
    private Integer timeLimitMinutes;
    private Double passingScorePercentage;
    private Integer maxAttempts;
    private com.hust.commonlibrary.entity.QuizType type;
    private Double bestScore;
    private Boolean isPassed;
    private com.hust.assessmentservice.entity.QuizStatus status; // NOTDONE, PROGRESS, DONE
    private Integer usedAttempts; // Lượt làm đã sử dụng
    private Integer questionsCount; // Tổng số câu hỏi
    private Instant createdAt;
}
