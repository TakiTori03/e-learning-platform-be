package com.hust.commonlibrary.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SttRequestEvent {
    private String mediaId;
    private String courseId;
    private String lessonId;
    private String audioKey;
    private String language;
}
