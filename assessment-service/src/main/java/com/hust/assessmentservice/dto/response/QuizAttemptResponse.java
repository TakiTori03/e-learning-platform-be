package com.hust.assessmentservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.List;
import com.hust.commonlibrary.entity.QuizType;
import com.hust.assessmentservice.entity.UserAnswer;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizAttemptResponse {
    private String id;
    private String userId;
    private String quizId;
    private String courseId;
    private Double score;
    private Boolean isPassed;
    private QuizType quizType;
    private Instant submittedAt;
    private List<UserAnswer> submittedAnswers;
    private Integer maxAttempts;
    private Long attemptsUsed;
}
