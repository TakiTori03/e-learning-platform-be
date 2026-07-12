package com.hust.commonlibrary.event;

import com.hust.commonlibrary.entity.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawTextIngestedEvent {
    private String courseId;
    private String lessonId;
    private String mediaId;
    private String content;
    private ContentType contentType;
    private String sourceCitation;
    private Integer chunkIndex;
}
