package com.hust.learningservice.dto.request;

import lombok.Data;

@Data
public class CourseNoteRequest {
    private String courseId;
    private String lessonId;
    private String content;
    private Double videoTime;
    private Integer page;
}
