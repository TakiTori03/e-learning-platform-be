package com.hust.courseservice.controller.internal;

import java.util.List;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import com.hust.commonlibrary.dto.ApiResponse;

import com.hust.courseservice.dto.response.CourseResponse;

import com.hust.courseservice.repository.LessonRepository;
import com.hust.courseservice.service.CourseService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/internal/courses")
@RequiredArgsConstructor
public class CourseInternalController {

    private final LessonRepository lessonRepository;
    private final CourseService courseService;

    @GetMapping("/{courseId}/lesson-count")
    public ApiResponse<Long> getLessonCount(@PathVariable String courseId) {
        long count = lessonRepository.countByCourseId(courseId);
        return ApiResponse.<Long>builder()
                .success(true)
                .payload(count)
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<CourseResponse> getCourseDetail(@PathVariable String id) {
        return ApiResponse.<CourseResponse>builder()
                .success(true)
                .payload(courseService.detail(id))
                .build();
    }


    @PostMapping("/bulk")
    public ApiResponse<List<CourseResponse>> getBulkCourses(@RequestBody List<String> courseIds) {
        return ApiResponse.<List<CourseResponse>>builder()
                .success(true)
                .payload(courseService.getBulkCourses(courseIds))
                .build();
    }

    @GetMapping("/all-active")
    public ApiResponse<List<CourseResponse>> getAllActiveCourses() {
        return ApiResponse.<List<CourseResponse>>builder()
                .success(true)
                .payload(courseService.getAllActiveCourses())
                .build();
    }

    @GetMapping("/discounted-last-month")
    public ApiResponse<List<CourseResponse>> getDiscountedCoursesLastMonth(
            @org.springframework.web.bind.annotation.RequestParam java.time.Instant currentTime) {
        return ApiResponse.<List<CourseResponse>>builder()
                .success(true)
                .payload(courseService.getDiscountedCourses(currentTime))
                .build();
    }
}
