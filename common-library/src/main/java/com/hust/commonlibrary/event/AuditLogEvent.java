package com.hust.commonlibrary.event;

import lombok.*;

import java.time.Instant;

/**
 * Đối tượng DTO đại diện cho một sự kiện kiểm toán vừa diễn ra.
 * Sẽ được bắn bất đồng bộ thông qua ApplicationEventPublisher.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class AuditLogEvent {

    private String action;
    private String targetId;
    private String userId;
    private String clientIp;
    private String methodName;
    
    @Builder.Default
    private Instant timestamp = Instant.now();
    
    private boolean success;
    private String errorMessage;
    private String description;
}
