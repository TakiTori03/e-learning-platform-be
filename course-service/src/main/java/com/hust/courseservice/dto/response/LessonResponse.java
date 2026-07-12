package com.hust.courseservice.dto.response;

import com.hust.commonlibrary.dto.TimeResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LessonResponse extends TimeResponse {
    private String id;
    private String name;
    private String description;
    private String content; 
    private Double videoLength;
    private String transcriptUrl;
    private String type; 
    private Integer position;
    private String sectionId;
    private String courseId;
}
