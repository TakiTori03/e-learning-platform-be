package com.hust.courseservice.controller.internal;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.courseservice.dto.response.LessonResponse;
import com.hust.courseservice.service.LessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/lessons")
@RequiredArgsConstructor
public class LessonInternalController {

    private final LessonService lessonService;

    @GetMapping("/{id}")
    public ApiResponse<LessonResponse> getLessonDetail(@PathVariable String id) {
        return ApiResponse.<LessonResponse>builder()
                .success(true)
                .payload(lessonService.detail(id))
                .build();
    }
}
