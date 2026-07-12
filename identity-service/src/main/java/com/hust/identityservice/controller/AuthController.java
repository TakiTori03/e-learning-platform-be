package com.hust.identityservice.controller;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.identityservice.dto.request.ChangePasswordRequest;
import com.hust.identityservice.dto.request.InstructorRegistrationRequest;
import com.hust.identityservice.dto.request.LoginRequest;
import com.hust.identityservice.dto.request.UserRegistrationRequest;
import com.hust.identityservice.dto.response.UserResponse;
import com.hust.identityservice.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserResponse>> login(@RequestBody @Valid LoginRequest request, HttpServletResponse response) {
        return ResponseEntity.ok(
                ApiResponse.<UserResponse>builder()
                        .success(true)
                        .payload(authService.login(request, response))
                        .build()
        );
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@RequestBody @Valid UserRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<UserResponse>builder()
                        .success(true)
                        .payload(authService.register(request))
                        .build()
        );
    }

    @PostMapping("/register-instructor")
    public ResponseEntity<ApiResponse<UserResponse>> registerInstructor(@RequestBody @Valid InstructorRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<UserResponse>builder()
                        .success(true)
                        .payload(authService.registerInstructor(request))
                        .build()
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .build()
        );
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestBody @Valid ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Password changed successfully")
                        .build()
        );
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<Void>> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        authService.refreshToken(request, response);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .build()
        );
    }
}
