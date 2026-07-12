package com.hust.commonlibrary.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CourseProgressInternalResponse {
    private String userId;
    private String courseId;
    private List<String> finishedLessonIds;
    private Double progress;
    private Boolean isEnrolled;
    private String lastAccessedLessonId;
}
