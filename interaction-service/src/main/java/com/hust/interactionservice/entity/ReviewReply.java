package com.hust.interactionservice.entity;

import com.hust.commonlibrary.entity.BaseDocument;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "review_replies")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewReply extends BaseDocument {
    @org.springframework.data.mongodb.core.index.Indexed
    private String reviewId;
    private String userId;
    private String content;
    private String code;
}
