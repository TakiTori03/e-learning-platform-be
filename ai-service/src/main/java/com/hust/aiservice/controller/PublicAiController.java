package com.hust.aiservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hust.aiservice.service.SemanticCacheService;
import com.hust.commonlibrary.dto.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/public/ai")
@RequiredArgsConstructor
@Slf4j
public class PublicAiController {

    private final SemanticCacheService semanticCacheService;

    @DeleteMapping("/cache")
    public ResponseEntity<ApiResponse<Void>> clearCache(
            @RequestParam(value = "courseId", required = false) String courseId) {
        log.info("📞 [Internal API] Nhận yêu cầu dọn dẹp cache cho courseId: {}", courseId);
        semanticCacheService.clearCache(courseId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Đã dọn dẹp cache L1 và L2 cho khóa học thành công.")
                .build());
    }
}
