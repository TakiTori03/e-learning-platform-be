package com.hust.courseservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.courseservice.service.SetupService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/public/setup")
@RequiredArgsConstructor
@Slf4j
public class SetupController {

    private final SetupService setupService;

    @PostMapping("/seed-courses")
    public ResponseEntity<ApiResponse<Void>> seedCourses() {
        log.info("REST request to seed mock courses received");
        setupService.seedCourses();
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Mock courses seeded successfully")
                        .build()
        );
    }
}
