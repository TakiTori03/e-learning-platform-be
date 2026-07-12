package com.hust.learningservice.controller;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.commonlibrary.utils.SecurityUtils;
import com.hust.learningservice.dto.response.EnrolledCourseSelectResponse;
import com.hust.learningservice.service.LearningService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final LearningService learningService;

    @GetMapping("/my-courses-select")
    public ApiResponse<List<EnrolledCourseSelectResponse>> getMyCoursesSelect(
            @RequestParam(required = false) String q) {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        List<EnrolledCourseSelectResponse> payload = learningService.getEnrolledCoursesSelect(userId, q);
        return ApiResponse.<List<EnrolledCourseSelectResponse>>builder()
                .success(true)
                .payload(payload)
                .build();
    }


    @GetMapping("/mine")
    public ApiResponse<List<com.hust.learningservice.entity.StudentEnrollment>> getMyEnrolledCourses() {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        return ApiResponse.<List<com.hust.learningservice.entity.StudentEnrollment>>builder()
                .success(true)
                .payload(learningService.getEnrolledCourses(userId))
                .build();
    }
}
