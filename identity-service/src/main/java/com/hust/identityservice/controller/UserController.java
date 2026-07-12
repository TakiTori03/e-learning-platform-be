package com.hust.identityservice.controller;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.commonlibrary.utils.SecurityUtils;
import com.hust.identityservice.dto.request.UserUpdateRequest;
import com.hust.identityservice.dto.response.UserResponse;
import com.hust.identityservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyInfo() {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        return ResponseEntity.ok(
                ApiResponse.<UserResponse>builder()
                        .success(true)
                        .payload(userService.getUserById(userId))
                        .build()
        );
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyProfile(
            @RequestBody UserUpdateRequest request) {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        return ResponseEntity.ok(
                ApiResponse.<UserResponse>builder()
                        .success(true)
                        .payload(userService.updateMyProfile(userId, request))
                        .build()
        );
    }

    @PatchMapping("/{id}/last-login")
    public ResponseEntity<ApiResponse<Void>> updateLastLogin(@PathVariable String id) {
        userService.updateLastLogin(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .build()
        );
    }

    @GetMapping("/authors/select")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getInstructorsSelect(
            @RequestParam(name = "q", required = false) String q) {
        return ResponseEntity.ok(
                ApiResponse.<List<UserResponse>>builder()
                        .success(true)
                        .payload(userService.getInstructorsSelect(q))
                        .build()
        );
    }

}
