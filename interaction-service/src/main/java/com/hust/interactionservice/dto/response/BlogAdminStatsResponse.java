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
public class BlogAdminStatsResponse {
    private long totalBlogs;
    private long publishedBlogs;
    private long blockedBlogs;
    private long reportedBlogs;
}
