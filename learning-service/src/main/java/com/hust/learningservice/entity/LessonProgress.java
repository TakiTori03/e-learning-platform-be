package com.hust.learningservice.entity;

import com.hust.commonlibrary.entity.BaseDocument;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "lesson_progress")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@CompoundIndex(name = "user_course_lesson_idx", def = "{'userId': 1, 'courseId': 1, 'lessonId': 1}", unique = true)
public class LessonProgress extends BaseDocument {

    @Indexed
    private String userId;

    @Indexed
    private String courseId;

    private String lessonId;

    private Boolean isDone;
    
    private Double lastWatchedTime;
}
