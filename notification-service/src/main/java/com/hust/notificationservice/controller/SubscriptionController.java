package com.hust.notificationservice.controller;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.commonlibrary.utils.SecurityUtils;
import com.hust.notificationservice.entity.Subscription;
import com.hust.notificationservice.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    public ApiResponse<Subscription> subscribe(@RequestParam String email) {
        String userId = SecurityUtils.getCurrentUserId().orElse(null);
        return ApiResponse.<Subscription>builder()
                .success(true)
                .payload(subscriptionService.subscribe(email, userId))
                .build();
    }

    @DeleteMapping("/{email}")
    public ApiResponse<Void> unsubscribe(@PathVariable String email) {
        subscriptionService.unsubscribe(email);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Unsubscribed successfully")
                .build();
    }

    @GetMapping("/me")
    public ApiResponse<Subscription> getMySubscription() {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        return ApiResponse.<Subscription>builder()
                .success(true)
                .payload(subscriptionService.getMySubscription(userId))
                .build();
    }
}
