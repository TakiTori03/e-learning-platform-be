package com.hust.interactionservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogInstructorStatsResponse {
    private long totalBlogs;
    private long publishedBlogs;
    private long draftBlogs;
    private long accumulatedViews;
    private long accumulatedLikes;
}
