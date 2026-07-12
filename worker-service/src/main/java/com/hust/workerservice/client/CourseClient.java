package com.hust.workerservice.client;

import com.hust.commonlibrary.dto.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "course-service", path = "/internal/courses")
public interface CourseClient {

    @GetMapping("/{id}")
    ApiResponse<CourseResponse> getCourseDetail(@PathVariable("id") String id);

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class CourseResponse {
        private String id;
        private String name;
        private String instructorId;
    }
}
