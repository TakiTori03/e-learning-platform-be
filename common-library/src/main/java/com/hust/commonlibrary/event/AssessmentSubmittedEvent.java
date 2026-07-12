package com.hust.commonlibrary.event;

import java.time.Instant;

import com.hust.commonlibrary.entity.QuizType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Sự kiện phát ra từ assessment-service khi học viên nộp bài làm trắc nghiệm.
 * learning-service lắng nghe để tự động cập nhật tiến độ bài học.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentSubmittedEvent {
    private String attemptId;
    private String userId;
    private String quizId;
    private String courseId;
    private String lessonId;
    private Double score;
    private Boolean isPassed;
    private QuizType quizType;
    private Instant submittedAt;
}
