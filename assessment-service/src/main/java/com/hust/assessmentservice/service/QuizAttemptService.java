package com.hust.assessmentservice.service;

import com.hust.assessmentservice.dto.response.QuizAttemptResponse;
import com.hust.assessmentservice.dto.request.QuizSubmitRequest;
import com.hust.assessmentservice.dto.response.QuizStartResponse;
import java.util.List;

public interface QuizAttemptService {
    QuizStartResponse startQuiz(String userId, String quizId);
    QuizAttemptResponse submitQuiz(String userId, String attemptId, QuizSubmitRequest request);
    List<QuizAttemptResponse> getAttemptsByUserIdAndQuizId(String userId, String quizId);
    List<QuizAttemptResponse> getAttemptsByUserIdAndCourseId(String userId, String courseId);
    com.hust.assessmentservice.dto.response.QuizEligibilityResponse getQuizEligibility(String userId, String quizId);
}
