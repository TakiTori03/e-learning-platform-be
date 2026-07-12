package com.hust.commonlibrary.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonMediaReadyEvent {
    private String lessonId;
    private String mediaId;
    private String hlsFolderName;
    private String transcriptUrl;
    private String url;
    private Long fileSize;
    private Double duration;
}
