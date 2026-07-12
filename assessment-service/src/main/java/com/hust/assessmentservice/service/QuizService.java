package com.hust.assessmentservice.service;

import com.hust.assessmentservice.dto.request.QuizRequest;
import com.hust.assessmentservice.dto.response.QuizResponse;
import com.hust.commonlibrary.dto.ListResponse;

import org.springframework.data.domain.Pageable;

public interface QuizService {
    QuizResponse createQuiz(QuizRequest request);
    QuizResponse updateQuiz(String id, QuizRequest request);
    void deleteQuiz(String id);
    QuizResponse getQuizById(String id, boolean isStudent);
    QuizResponse getQuizByLessonId(String lessonId, boolean isStudent);
    ListResponse<QuizResponse> getAllQuizzes(String authorId, String q, String courseId);
    ListResponse<QuizResponse> getQuizzesForSelect(String authorId, String q, com.hust.commonlibrary.entity.QuizType type);
    com.hust.assessmentservice.dto.response.QuizStatsResponse getQuizStatistics(String authorId);
    ListResponse<com.hust.assessmentservice.dto.response.QuizAssignmentResponse> getStudentAssignmentsPaged(
            String userId, java.util.List<String> courseIds, String search, com.hust.commonlibrary.entity.QuizType type, com.hust.assessmentservice.entity.QuizStatus status, Pageable pageable
    );
    com.hust.assessmentservice.dto.response.QuizAssignmentStatsResponse getStudentAssignmentsStats(
            String userId, java.util.List<String> courseIds
    );
    QuizResponse importQuiz(String title, String description, Integer timeLimitMinutes, Double passingScorePercentage, com.hust.commonlibrary.entity.QuizType type, Integer maxAttempts, org.springframework.web.multipart.MultipartFile file, String authorId);
}
