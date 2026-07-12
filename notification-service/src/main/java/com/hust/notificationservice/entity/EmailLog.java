package com.hust.notificationservice.entity;

import com.hust.commonlibrary.entity.BaseDocument;
import com.hust.notificationservice.entity.enums.EmailStatus;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "email_logs")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EmailLog extends BaseDocument {

    @Indexed
    private String recipientEmail;

    private String subject;

    private EmailStatus status; // SENT, FAILED

    private String errorMessage; // Lưu vết lỗi nếu gửi thất bại
}
