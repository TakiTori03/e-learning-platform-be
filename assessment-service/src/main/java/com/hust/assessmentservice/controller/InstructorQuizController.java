package com.hust.assessmentservice.controller;

import com.hust.assessmentservice.dto.request.QuizRequest;
import com.hust.assessmentservice.dto.response.QuizResponse;
import com.hust.assessmentservice.service.QuizService;
import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.commonlibrary.dto.ListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/instructor/quizzes")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
public class InstructorQuizController {

    private final QuizService quizService;

    @PostMapping
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<QuizResponse>> createQuiz(@RequestBody QuizRequest request) {
        log.info("Instructor API: Creating new quiz");
        String authorId = com.hust.commonlibrary.utils.SecurityUtils.getCurrentUserIdOrThrow();
        request.setAuthorId(authorId);
        QuizResponse response = quizService.createQuiz(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<QuizResponse>builder()
                        .success(true)
                        .payload(response)
                        .build()
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<QuizResponse>> updateQuiz(@PathVariable String id, @RequestBody QuizRequest request) {
        log.info("Instructor API: Updating quiz id={}", id);
        QuizResponse response = quizService.updateQuiz(id, request);
        return ResponseEntity.ok(
                ApiResponse.<QuizResponse>builder()
                        .success(true)
                        .payload(response)
                        .build()
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<Void>> deleteQuiz(@PathVariable String id) {
        log.info("Instructor API: Deleting quiz id={}", id);
        quizService.deleteQuiz(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Quiz deleted successfully")
                        .build()
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<QuizResponse>> getQuizById(@PathVariable String id) {
        log.info("Instructor/Admin API: Fetching quiz id={}", id);
        QuizResponse response = quizService.getQuizById(id, false); // false: show correct answers
        return ResponseEntity.ok(
                ApiResponse.<QuizResponse>builder()
                        .success(true)
                        .payload(response)
                        .build()
        );
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<com.hust.assessmentservice.dto.response.QuizStatsResponse>> getQuizStatistics() {
        log.info("Instructor API: Fetching quiz statistics");
        String authorId = com.hust.commonlibrary.utils.SecurityUtils.getCurrentUserIdOrThrow();
        com.hust.assessmentservice.dto.response.QuizStatsResponse response = quizService.getQuizStatistics(authorId);
        return ResponseEntity.ok(
                ApiResponse.<com.hust.assessmentservice.dto.response.QuizStatsResponse>builder()
                        .success(true)
                        .payload(response)
                        .build()
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<ListResponse<QuizResponse>>> getAllQuizzes(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String courseId) {
        log.info("Instructor API: Fetching all quizzes with q={}, courseId={}", q, courseId);
        String authorId = com.hust.commonlibrary.utils.SecurityUtils.getCurrentUserIdOrThrow();
        ListResponse<QuizResponse> listResponse = quizService.getAllQuizzes(authorId, q, courseId);

        return ResponseEntity.ok(
                ApiResponse.<ListResponse<QuizResponse>>builder()
                        .success(true)
                        .payload(listResponse)
                        .build()
        );
    }

    @GetMapping("/select")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<ListResponse<QuizResponse>>> getQuizzesForSelect(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) com.hust.commonlibrary.entity.QuizType type) {
        log.info("Instructor API: Fetching quizzes for select with q={}, type={}", q, type);
        String authorId = com.hust.commonlibrary.utils.SecurityUtils.getCurrentUserIdOrThrow();
        ListResponse<QuizResponse> listResponse = quizService.getQuizzesForSelect(authorId, q, type);
        return ResponseEntity.ok(
                ApiResponse.<ListResponse<QuizResponse>>builder()
                        .success(true)
                        .payload(listResponse)
                        .build()
        );
    }

    @PostMapping("/import")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<QuizResponse>> importQuiz(
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "timeLimitMinutes", required = false) Integer timeLimitMinutes,
            @RequestParam(value = "passingScorePercentage", required = false) Double passingScorePercentage,
            @RequestParam(value = "type", required = false) com.hust.commonlibrary.entity.QuizType type,
            @RequestParam(value = "maxAttempts", required = false) Integer maxAttempts,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        log.info("Instructor API: Importing quiz from file");
        String authorId = com.hust.commonlibrary.utils.SecurityUtils.getCurrentUserIdOrThrow();
        QuizResponse response = quizService.importQuiz(title, description, timeLimitMinutes, passingScorePercentage, type, maxAttempts, file, authorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<QuizResponse>builder()
                        .success(true)
                        .payload(response)
                        .build()
        );
    }
}
