package com.hust.identityservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.identityservice.service.SetupService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/public/setup")
@RequiredArgsConstructor
@Slf4j
public class SetupController {

    private final SetupService setupService;

    @PostMapping("/seed-users")
    public ResponseEntity<ApiResponse<Void>> seedUsers() {
        log.info("REST request to seed mock users received");
        setupService.seedUsers();
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Mock users seeded successfully")
                        .build()
        );
    }
}
