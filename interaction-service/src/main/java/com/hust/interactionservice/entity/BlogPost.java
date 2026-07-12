package com.hust.interactionservice.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import com.hust.commonlibrary.entity.BaseDocument;
import com.hust.interactionservice.entity.enums.BlogStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Document(collection = "blog_posts")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
public class BlogPost extends BaseDocument {

    private String title;

    @Indexed(unique = true)
    private String slug;

    private String content;
    private String thumbnail;

    @Indexed
    @DocumentReference(lazy = true)
    private BlogTopic topic;

    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Indexed
    private BlogStatus status;

    private Boolean isPinned;

    @Builder.Default
    private List<String> likes = new ArrayList<>();

    @Builder.Default
    private List<ReportInfo> reports = new ArrayList<>();

    @Builder.Default
    private Long viewsCount = 0L;

    @Builder.Default
    private Long commentsCount = 0L;

    @Builder.Default
    private Long sharesCount = 0L;

    @Builder.Default
    private List<String> courseIds = new ArrayList<>();

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportInfo {
        private String userId;
        private String reason;
        private Instant createdAt;
    }
}
