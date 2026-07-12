package com.hust.notificationservice.client;

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

    @GetMapping("/discounted-last-month")
    ApiResponse<java.util.List<CourseResponse>> getDiscountedCoursesLastMonth(@org.springframework.web.bind.annotation.RequestParam("currentTime") java.time.Instant currentTime);

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class CourseResponse {
        private String id;
        private String name;
        private String instructorId;
        private Double price;
        private Double finalPrice;
        private String courseSlug;
        private String thumbnail;
    }
}
