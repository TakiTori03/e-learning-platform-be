package com.hust.courseservice.dto.response;

import com.hust.commonlibrary.dto.TimeResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SectionResponse extends TimeResponse {
    private String id;
    private String courseId;
    private String name;
    private String description;
    private Integer position;
    private List<LessonResponse> lessons;
}
