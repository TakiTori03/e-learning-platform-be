package com.hust.interactionservice.entity;

import com.hust.commonlibrary.entity.BaseDocument;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "blog_comments")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
public class BlogComment extends BaseDocument {

    @Indexed
    private String blogId;

    private String content;

    @Indexed
    private String parentCommentId;
}
