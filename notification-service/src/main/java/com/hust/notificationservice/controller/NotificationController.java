package com.hust.notificationservice.controller;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.commonlibrary.dto.ListResponse;
import com.hust.commonlibrary.utils.SecurityUtils;
import com.hust.notificationservice.entity.Notification;
import com.hust.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<ListResponse<Notification>> getNotifications(Pageable pageable) {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        Page<Notification> page = notificationService.getNotifications(userId, pageable);
        return ApiResponse.<ListResponse<Notification>>builder()
                .success(true)
                .payload(ListResponse.of(page.getContent(), page))
                .build();
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> getUnreadCount() {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        return ApiResponse.<Long>builder()
                .success(true)
                .payload(notificationService.getUnreadCount(userId))
                .build();
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Notification> markAsRead(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        return ApiResponse.<Notification>builder()
                .success(true)
                .payload(notificationService.markAsRead(id, userId))
                .build();
    }

    @PutMapping("/read-all")
    public ApiResponse<Void> markAllAsRead() {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        notificationService.markAllAsRead(userId);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("All notifications marked as read")
                .build();
    }
}
