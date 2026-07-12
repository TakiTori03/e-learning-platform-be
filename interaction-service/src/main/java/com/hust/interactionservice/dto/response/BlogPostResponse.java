package com.hust.interactionservice.dto.response;

import java.util.List;

import com.hust.commonlibrary.dto.TimeResponse;
import com.hust.commonlibrary.dto.UserSharedProfile;
import com.hust.interactionservice.entity.enums.BlogStatus;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BlogPostResponse extends TimeResponse {
    private String id;
    private String title;
    private String slug;
    private String content;
    private String thumbnail;
    private BlogTopicResponse topic;
    private BlogStatus status;
    private Boolean isPinned;
    private List<String> tags;
    private List<String> courseIds;

    private Long sharesCount;

    private Long likesCount;
    private Long commentsCount;
    private Long reportsCount;
    private Long viewsCount;

    private Boolean isLiked;
    private UserSharedProfile author;

    private String createdBy;
}
