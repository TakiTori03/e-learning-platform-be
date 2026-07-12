package com.hust.interactionservice.entity;

import com.hust.commonlibrary.entity.BaseDocument;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "feedback_replies")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackReply extends BaseDocument {
    private String feedbackId;
    private String content;
}
