package com.hust.learningservice.client;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.commonlibrary.dto.LessonInternalResponse;

@FeignClient(name = "course-service")
public interface CourseClient {

    @GetMapping("/internal/courses/{courseId}/lesson-count")
    ApiResponse<Long> getLessonCount(@PathVariable String courseId);

    @GetMapping("/internal/lessons/{lessonId}")
    ApiResponse<LessonInternalResponse> getLessonDetail(@PathVariable String lessonId);

    @GetMapping("/internal/courses/{id}")
    ApiResponse<com.hust.commonlibrary.dto.CourseInternalResponse> getCourseDetail(@PathVariable String id);

    @PostMapping("/internal/courses/bulk")
    ApiResponse<List<com.hust.commonlibrary.dto.CourseInternalResponse>> getBulkCourses(@RequestBody List<String> courseIds);
}
