package com.hust.interactionservice.dto.response;

import com.hust.commonlibrary.dto.TimeResponse;
import com.hust.commonlibrary.dto.UserSharedProfile;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BlogCommentResponse extends TimeResponse {
    private String id;
    private String blogId;
    private String content;
    private String parentCommentId;
    private UserSharedProfile author;
    
    private String createdBy;

    @Builder.Default
    private List<BlogCommentResponse> replies = new ArrayList<>();
}
