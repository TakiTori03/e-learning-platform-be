package com.hust.notificationservice.entity;

import com.hust.commonlibrary.entity.BaseDocument;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "subscriptions")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Subscription extends BaseDocument {

    @Indexed
    private String userId; // Có thể null nếu khách vãng lai

    @Indexed(unique = true)
    private String email;

    private List<String> topics; // Ví dụ: ["NEWSLETTER", "COURSE_UPDATE"]

    @Builder.Default
    private Boolean isActive = true;
}
