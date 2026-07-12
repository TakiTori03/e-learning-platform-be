package com.hust.orderservice.client;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.orderservice.client.dto.CourseInternalResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "course-service")
public interface CourseClient {

    @GetMapping("/internal/courses/{id}")
    ApiResponse<CourseInternalResponse> getCourseDetail(@PathVariable String id);
}
