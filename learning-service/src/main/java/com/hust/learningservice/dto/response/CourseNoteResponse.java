package com.hust.learningservice.dto.response;

import com.hust.commonlibrary.dto.TimeResponse;
import lombok.*;
import lombok.experimental.SuperBuilder;


@EqualsAndHashCode(callSuper = true)
@Getter @Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CourseNoteResponse extends TimeResponse {
    private String id;
    private String lessonId;
    private String content;
    private Double videoTime;
    private Integer page;
}
