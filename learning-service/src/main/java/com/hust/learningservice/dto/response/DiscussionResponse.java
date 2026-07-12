package com.hust.learningservice.dto.response;

import com.hust.commonlibrary.dto.TimeResponse;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Getter @Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DiscussionResponse extends TimeResponse {
    private String id;
    private String code;
    private String courseId;
    private String sectionId;
    private String lessonId;
    private String userId;
    private String content;
    private String parentId;
    private String rootId;
    private List<DiscussionResponse> replies;
    private List<String> likedUserIds;
    private List<String> dislikedUserIds;
    private int replyCount;
    private com.hust.commonlibrary.dto.UserSharedProfile user;
}
