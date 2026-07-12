package com.hust.aiservice.dto;

import com.hust.commonlibrary.entity.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultDTO {
    private String id;
    private String courseId;
    private String lessonId;
    private String content;
    private ContentType contentType;
    private String sourceCitation;
    private double score;
    private String mediaId;
    private Integer chunkIndex;
}
