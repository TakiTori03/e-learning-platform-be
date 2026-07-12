package com.hust.learningservice.controller.internal;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.learningservice.service.LearningService;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/enrollments")
@RequiredArgsConstructor
public class EnrollmentInternalController {

    private final LearningService enrollmentService;

    @GetMapping("/check-lesson-access")
    public ApiResponse<Boolean> checkLessonAccess(
            @RequestParam String userId,
            @RequestParam String lessonId) {
        
        boolean hasAccess = enrollmentService.checkLessonAccess(userId, lessonId);
        
        return ApiResponse.<Boolean>builder()
                .success(true)
                .payload(hasAccess)
                .build();
    }
}
