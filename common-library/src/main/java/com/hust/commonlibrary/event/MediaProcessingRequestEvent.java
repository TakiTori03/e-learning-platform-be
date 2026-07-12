package com.hust.commonlibrary.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaProcessingRequestEvent {
    private String mediaId;
    private String fileUrl;
    private String courseId;
    private String lessonId;
    private String mediaType;
}
