package com.hust.notificationservice.service;

import com.hust.notificationservice.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    Page<Notification> getNotifications(String userId, Pageable pageable);
    long getUnreadCount(String userId);
    Notification markAsRead(String id, String userId);
    void markAllAsRead(String userId);
}
