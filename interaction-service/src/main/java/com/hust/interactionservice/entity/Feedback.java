package com.hust.interactionservice.entity;

import com.hust.commonlibrary.entity.BaseDocument;
import com.hust.interactionservice.constant.FeedbackStatus;
import com.hust.interactionservice.constant.FeedbackType;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "feedbacks")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Feedback extends BaseDocument {
    private String name;
    private String email;
    @org.springframework.data.mongodb.core.index.Indexed
    private FeedbackType type;
    private String title;
    private String content;
    @org.springframework.data.mongodb.core.index.Indexed
    private FeedbackStatus status;
}
