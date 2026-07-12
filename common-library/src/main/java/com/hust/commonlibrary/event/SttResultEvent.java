package com.hust.commonlibrary.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SttResultEvent {
    private String mediaId;
    private String courseId;
    private String lessonId;
    private String vttContent;
    private String hlsFolderName;
    private Double duration;
    private boolean success;
    private String errorMessage;
}
