package com.hust.learningservice.entity;

import com.hust.commonlibrary.entity.BaseDocument;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "student_enrollments")
@CompoundIndexes({
    @CompoundIndex(name = "user_course_unique_idx", def = "{'userId': 1, 'courseId': 1}", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class StudentEnrollment extends BaseDocument {

    private String userId;

    @Indexed
    private String courseId;

    private String orderId;
    private Double progress;
    
    private Boolean isCompleted;
    private Instant completedAt;

    private String lastAccessedLessonId;
}
