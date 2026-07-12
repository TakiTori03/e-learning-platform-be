package com.hust.learningservice.controller;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.commonlibrary.utils.SecurityUtils;
import com.hust.learningservice.dto.request.LessonProgressRequest;
import com.hust.learningservice.dto.response.CourseProgressResponse;
import com.hust.learningservice.service.LearningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final LearningService learningService;

    @PostMapping("/track")
    public ApiResponse<Void> trackProgress(@RequestBody @Valid LessonProgressRequest request) {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        learningService.trackProgress(userId, request);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }

    @PostMapping("/access")
    public ApiResponse<Void> updateLastAccessed(
            @RequestParam String courseId,
            @RequestParam String lessonId) {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        learningService.updateLastAccessedLesson(userId, courseId, lessonId);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }

    @GetMapping("/{courseId}")
    public ApiResponse<CourseProgressResponse> getMyProgress(@PathVariable String courseId) {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        return ApiResponse.<CourseProgressResponse>builder()
                .success(true)
                .payload(learningService.getCourseProgress(userId, courseId))
                .build();
    }

    @GetMapping("/bulk/mine")
    public ApiResponse<java.util.Map<String, CourseProgressResponse>> getMyProgressBulk(
            @RequestParam java.util.List<String> courseIds) {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        return ApiResponse.<java.util.Map<String, CourseProgressResponse>>builder()
                .success(true)
                .payload(learningService.getCourseProgressBulk(userId, courseIds))
                .build();
    }
}
