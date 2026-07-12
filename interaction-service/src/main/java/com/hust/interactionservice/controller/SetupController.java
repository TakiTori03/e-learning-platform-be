package com.hust.interactionservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.interactionservice.service.SetupService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/public/setup")
@RequiredArgsConstructor
public class SetupController {

    private final SetupService setupService;

    @PostMapping("/seed-blogs")
    public ResponseEntity<ApiResponse<Void>> seedBlogs() {
        setupService.seedBlogs();
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Seeded blogs successfully")
                        .build()
        );
    }
}
