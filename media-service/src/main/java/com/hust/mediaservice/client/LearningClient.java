package com.hust.mediaservice.client;

import com.hust.commonlibrary.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "learning-service")
public interface LearningClient {

    @GetMapping("/internal/enrollments/check-lesson-access")
    ApiResponse<Boolean> checkLessonAccess(
            @RequestParam("userId") String userId, 
            @RequestParam("lessonId") String lessonId);
}
