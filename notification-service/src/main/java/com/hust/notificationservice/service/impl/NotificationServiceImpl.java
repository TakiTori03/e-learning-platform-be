package com.hust.notificationservice.service.impl;

import com.hust.commonlibrary.constants.AppConstants;
import com.hust.commonlibrary.utils.SecurityUtils;
import com.hust.notificationservice.entity.Notification;
import com.hust.notificationservice.repository.NotificationRepository;
import com.hust.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public Page<Notification> getNotifications(String userId, Pageable pageable) {
        List<String> userIds = SecurityUtils.isAdmin() ? List.of(userId, AppConstants.Notification_Constants.ALL_ADMIN_TOPIC) : List.of(userId);
        return notificationRepository.findAllByUserIdInOrderByCreatedAtDesc(userIds, pageable);
    }

    @Override
    public long getUnreadCount(String userId) {
        List<String> userIds = SecurityUtils.isAdmin() ? List.of(userId, AppConstants.Notification_Constants.ALL_ADMIN_TOPIC) : List.of(userId);
        return notificationRepository.countByUserIdInAndIsReadFalse(userIds);
    }

    @Override
    public Notification markAsRead(String id, String userId) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + id));

        List<String> userIds = SecurityUtils.isAdmin() ? List.of(userId, AppConstants.Notification_Constants.ALL_ADMIN_TOPIC) : List.of(userId);
        if (!userIds.contains(notification.getUserId())) {
            throw new RuntimeException("Unauthorized access to notification");
        }

        notification.setIsRead(true);
        return notificationRepository.save(notification);
    }

    @Override
    public void markAllAsRead(String userId) {
        List<String> userIds = SecurityUtils.isAdmin() ? List.of(userId, AppConstants.Notification_Constants.ALL_ADMIN_TOPIC) : List.of(userId);
        List<Notification> unread = notificationRepository.findAllByUserIdInAndIsReadFalse(userIds);
        unread.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unread);
    }
}
