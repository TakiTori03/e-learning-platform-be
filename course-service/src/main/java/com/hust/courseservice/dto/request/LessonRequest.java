package com.hust.courseservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonRequest {
    private String name;
    private String description;
    private String content;
    private String type; 
    private Integer position;
    private String sectionId;
    private String courseId;
}
