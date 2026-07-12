package com.hust.courseservice.entity;

import com.hust.commonlibrary.entity.BaseDocument;
import com.hust.courseservice.entity.enums.LessonType;
import lombok.*;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "lessons")
@CompoundIndexes({
    @CompoundIndex(name = "section_position_idx", def = "{'sectionId': 1, 'position': 1}"),
    @CompoundIndex(name = "course_position_idx", def = "{'courseId': 1, 'position': 1}")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Lesson extends BaseDocument {
    private String sectionId;
    private String courseId;

    @TextIndexed
    private String name;
    private String description;

  
    private String content; 
    private Double videoLength;
    private String transcriptUrl;

    private LessonType type; 
    private Integer position;
}
