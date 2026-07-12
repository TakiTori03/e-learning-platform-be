package com.hust.assessmentservice.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import com.hust.commonlibrary.dto.ListResponse;
import com.hust.assessmentservice.dto.response.QuizAssignmentResponse;

import com.hust.assessmentservice.dto.request.QuizSubmitRequest;
import com.hust.assessmentservice.dto.response.QuizAttemptResponse;
import com.hust.assessmentservice.dto.response.QuizResponse;
import com.hust.assessmentservice.dto.response.QuizAssignmentStatsResponse;
import com.hust.assessmentservice.service.QuizAttemptService;
import com.hust.assessmentservice.service.QuizService;
import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.commonlibrary.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/quizzes")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('STUDENT')")
public class StudentQuizController {

    private final QuizService quizService;
    private final QuizAttemptService quizAttemptService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<QuizResponse>> getQuizForStudent(@PathVariable String id) {
        log.info("Student API: Fetching quiz id={}", id);
        QuizResponse response = quizService.getQuizById(id, true);
        return ResponseEntity.ok(
                ApiResponse.<QuizResponse>builder()
                        .success(true)
                        .payload(response)
                        .build()
        );
    }

    @GetMapping("/lesson/{lessonId}")
    public ResponseEntity<ApiResponse<QuizResponse>> getQuizByLessonId(@PathVariable String lessonId) {
        log.info("Student API: Fetching quiz by lessonId={}", lessonId);
        QuizResponse response = quizService.getQuizByLessonId(lessonId, true);
        return ResponseEntity.ok(
                ApiResponse.<QuizResponse>builder()
                        .success(true)
                        .payload(response)
                        .build()
        );
    }

    @PostMapping("/{quizId}/start")
    public ResponseEntity<ApiResponse<com.hust.assessmentservice.dto.response.QuizStartResponse>> startQuiz(@PathVariable String quizId) {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        log.info("Student API: Starting quiz for userId={}, quizId={}", userId, quizId);
        com.hust.assessmentservice.dto.response.QuizStartResponse response = quizAttemptService.startQuiz(userId, quizId);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<com.hust.assessmentservice.dto.response.QuizStartResponse>builder()
                        .success(true)
                        .payload(response)
                        .build()
        );
    }

    @PostMapping("/{attemptId}/submit")
    public ResponseEntity<ApiResponse<QuizAttemptResponse>> submitQuiz(
            @PathVariable String attemptId,
            @jakarta.validation.Valid @RequestBody QuizSubmitRequest request) {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        log.info("Student API: Submitting quiz attempt for userId={}, attemptId={}", userId, attemptId);
        QuizAttemptResponse response = quizAttemptService.submitQuiz(userId, attemptId, request);
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.<QuizAttemptResponse>builder()
                        .success(true)
                        .payload(response)
                        .build()
        );
    }

    @GetMapping("/{id}/attempts")
    public ResponseEntity<ApiResponse<List<QuizAttemptResponse>>> getMyAttemptsForQuiz(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        log.info("Student API: Fetching quiz attempts for userId={}, quizId={}", userId, id);
        List<QuizAttemptResponse> responses = quizAttemptService.getAttemptsByUserIdAndQuizId(userId, id);
        return ResponseEntity.ok(
                ApiResponse.<List<QuizAttemptResponse>>builder()
                        .success(true)
                        .payload(responses)
                        .build()
        );
    }

    @GetMapping("/course/{courseId}/attempts")
    public ResponseEntity<ApiResponse<List<QuizAttemptResponse>>> getMyAttemptsForCourse(@PathVariable String courseId) {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        log.info("Student API: Fetching course quiz attempts for userId={}, courseId={}", userId, courseId);
        List<QuizAttemptResponse> responses = quizAttemptService.getAttemptsByUserIdAndCourseId(userId, courseId);
        return ResponseEntity.ok(
                ApiResponse.<List<QuizAttemptResponse>>builder()
                        .success(true)
                        .payload(responses)
                        .build()
        );
    }

    @GetMapping("/{id}/eligibility")
    public ResponseEntity<ApiResponse<com.hust.assessmentservice.dto.response.QuizEligibilityResponse>> getQuizEligibility(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        log.info("Student API: Fetching quiz eligibility for userId={}, quizId={}", userId, id);
        com.hust.assessmentservice.dto.response.QuizEligibilityResponse response = quizAttemptService.getQuizEligibility(userId, id);
        return ResponseEntity.ok(
                ApiResponse.<com.hust.assessmentservice.dto.response.QuizEligibilityResponse>builder()
                        .success(true)
                        .payload(response)
                        .build()
        );
    }

    @GetMapping("/assignments/page")
    public ResponseEntity<ApiResponse<ListResponse<QuizAssignmentResponse>>> getStudentAssignmentsPaged(
            @RequestParam List<String> courseIds,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) com.hust.commonlibrary.entity.QuizType type,
            @RequestParam(required = false) com.hust.assessmentservice.entity.QuizStatus status,
            @PageableDefault Pageable pageable) {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        log.info("Student API: Fetching paged assignments for userId={}, courseIds={}, search={}, type={}, status={}, pageable={}",
                userId, courseIds, search, type, status, pageable);
        ListResponse<QuizAssignmentResponse> response =
                quizService.getStudentAssignmentsPaged(userId, courseIds, search, type, status, pageable);
        return ResponseEntity.ok(
                ApiResponse.<ListResponse<QuizAssignmentResponse>>builder()
                        .success(true)
                        .payload(response)
                        .build()
        );
    }

    @GetMapping("/assignments/stats")
    public ResponseEntity<ApiResponse<QuizAssignmentStatsResponse>> getStudentAssignmentsStats(
            @RequestParam List<String> courseIds) {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        log.info("Student API: Fetching assignments stats for userId={}, courseIds={}", userId, courseIds);
        QuizAssignmentStatsResponse response = quizService.getStudentAssignmentsStats(userId, courseIds);
        return ResponseEntity.ok(
                ApiResponse.<QuizAssignmentStatsResponse>builder()
                        .success(true)
                        .payload(response)
                        .build()
        );
    }
}
