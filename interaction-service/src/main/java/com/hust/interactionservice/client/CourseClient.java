package com.hust.interactionservice.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.commonlibrary.dto.CourseInternalResponse;

@FeignClient(name = "course-service")
public interface CourseClient {

    @GetMapping("/internal/courses/all-active")
    ApiResponse<List<CourseInternalResponse>> getAllActiveCourses();

    @org.springframework.web.bind.annotation.PostMapping("/internal/courses/bulk")
    ApiResponse<List<CourseInternalResponse>> getBulkCourses(@org.springframework.web.bind.annotation.RequestBody List<String> courseIds);
}
