package com.hust.commonlibrary.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    private String userId; // "ALL_ADMIN" for admin alerts, or specific userId
    private String title;
    private String message;
    private com.hust.commonlibrary.enums.NotificationType type; 
    private String redirectUrl; 
}
