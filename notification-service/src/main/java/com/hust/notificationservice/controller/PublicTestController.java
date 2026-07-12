package com.hust.notificationservice.controller;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.notificationservice.entity.Notification;
import com.hust.commonlibrary.enums.NotificationType;
import com.hust.notificationservice.repository.NotificationRepository;
import com.hust.notificationservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/public/test")
@RequiredArgsConstructor
public class PublicTestController {

    private final EmailService emailService;
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final com.hust.notificationservice.scheduler.NewsletterScheduler newsletterScheduler;

    @GetMapping("/email")
    public ApiResponse<String> testEmail(@RequestParam String to) {
        Map<String, Object> model = new HashMap<>();
        model.put("userFullName", "Học Viên Thử Nghiệm");
        model.put("content", "Đây là câu hỏi thử nghiệm gửi từ hệ thống E-Learning.");
        model.put("replyContent", "Chào bạn, đây là câu trả lời thử nghiệm từ quản trị viên!");
        model.put("repliedBy", "Admin Test");

        emailService.sendHtmlEmail(
                to,
                "Thử nghiệm email phản hồi ý kiến đóng góp",
                "feedback-reply-template",
                model
        );

        return ApiResponse.<String>builder()
                .success(true)
                .message("Email sent request queued successfully!")
                .payload("Sent to: " + to)
                .build();
    }

    @PostMapping("/notification")
    public ApiResponse<Notification> testNotification(
            @RequestParam String userId,
            @RequestParam String title,
            @RequestParam String message) {

        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .type(NotificationType.SYSTEM)
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);

        // Broadcast to websocket
        messagingTemplate.convertAndSend("/topic/user/" + userId + "/notifications", saved);

        return ApiResponse.<Notification>builder()
                .success(true)
                .message("Notification simulated and broadcasted successfully")
                .payload(saved)
                .build();
    }

    @GetMapping("/newsletter")
    public ApiResponse<String> testNewsletter(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate testDate) {
        
        java.time.Instant currentTime;
        if (testDate == null) {
            currentTime = java.time.Instant.now();
        } else {
            // Chuyển LocalDate (ví dụ: 2026-08-01) thành Instant bắt đầu của ngày hôm đó theo giờ UTC
            currentTime = testDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        }
        
        newsletterScheduler.processAndSendNewsletter(currentTime);
        
        return ApiResponse.<String>builder()
                .success(true)
                .message("Newsletter triggered successfully")
                .payload("Time context: " + currentTime)
                .build();
    }
}
