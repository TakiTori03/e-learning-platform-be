package com.hust.notificationservice.consumer;

import com.hust.commonlibrary.constants.AppConstants;
import com.hust.commonlibrary.event.FeedbackRepliedEvent;
import com.hust.commonlibrary.event.NotificationEvent;
import com.hust.notificationservice.entity.Notification;
import com.hust.commonlibrary.enums.NotificationType;
import com.hust.notificationservice.repository.NotificationRepository;
import com.hust.notificationservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class NotificationKafkaConsumer {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final SimpMessagingTemplate messagingTemplate;

    @KafkaListener(topics = "feedback-replied-topic", groupId = "notification-group")
    public void consumeFeedbackReplied(FeedbackRepliedEvent event) {
        log.info("Received feedback replied event for userEmail: {}", event.getUserEmail());
        try {
            Map<String, Object> templateModel = new HashMap<>();
            templateModel.put("userFullName", event.getUserFullName());
            templateModel.put("content", event.getContent());
            templateModel.put("replyContent", event.getReplyContent());
            templateModel.put("repliedBy", event.getRepliedBy());

            emailService.sendHtmlEmail(
                    event.getUserEmail(),
                    "Phản hồi ý kiến đóng góp từ E-Learning Platform",
                    "feedback-reply-template",
                    templateModel
            );
            log.info("Feedback reply email queued successfully for {}", event.getUserEmail());
        } catch (Exception e) {
            log.error("Error processing feedback reply email", e);
        }
    }

    @KafkaListener(topics = "notification-events-topic", groupId = "notification-group")
    public void consumeNotificationEvent(NotificationEvent event) {
        log.info("Received standard notification event for userId: {}", event.getUserId());
        try {
            NotificationType type = event.getType() != null ? event.getType() : NotificationType.SYSTEM;

            Notification notification = Notification.builder()
                    .userId(event.getUserId())
                    .title(event.getTitle())
                    .message(event.getMessage())
                    .type(type)
                    .redirectUrl(event.getRedirectUrl())
                    .isRead(false)
                    .build();

            Notification saved = notificationRepository.save(notification);

            // Push WebSocket notification
            if (AppConstants.Notification_Constants.ALL_ADMIN_TOPIC.equals(event.getUserId())) {
                messagingTemplate.convertAndSend("/topic/admin/notifications", saved);
                log.info("Notification saved and pushed via WebSocket to /topic/admin/notifications");
            } else {
                messagingTemplate.convertAndSend("/topic/user/" + event.getUserId() + "/notifications", saved);
                log.info("Notification saved and pushed via WebSocket for user: {}", event.getUserId());
            }
        } catch (Exception e) {
            log.error("Error processing standard notification event", e);
        }
    }
}
