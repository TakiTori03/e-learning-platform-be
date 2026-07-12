package com.hust.notificationservice.entity;

import com.hust.commonlibrary.entity.BaseDocument;
import com.hust.commonlibrary.enums.NotificationType;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "notifications")
@org.springframework.data.mongodb.core.index.CompoundIndexes({
    @org.springframework.data.mongodb.core.index.CompoundIndex(name = "created_at_idx", def = "{'createdAt': -1}"),
    @org.springframework.data.mongodb.core.index.CompoundIndex(name = "is_read_idx", def = "{'isRead': 1}")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Notification extends BaseDocument {

    @Indexed
    private String userId; // Bắt buộc, người nhận thông báo

    private String title;

    private String message;

    private String redirectUrl; // Link để redirect khi user click vào thông báo

    private NotificationType type; // SYSTEM, COURSE_UPDATE, ASYNC_TASK

    @Builder.Default
    private Boolean isRead = false;
}
