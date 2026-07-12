package com.hust.learningservice.entity;

import com.hust.commonlibrary.entity.BaseDocument;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "course_notes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CourseNote extends BaseDocument {

    @Indexed
    private String userId;

    @Indexed
    private String courseId;

    private String lessonId;

    private String content;

    private Double videoTime;

    private Integer page;
}
