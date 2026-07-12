package com.hust.learningservice.repository;

import com.hust.learningservice.entity.CourseNote;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface CourseNoteRepository extends MongoRepository<CourseNote, String> {
    List<CourseNote> findAllByUserIdAndCourseId(String userId, String courseId);
    List<CourseNote> findAllByUserIdAndLessonId(String userId, String lessonId);
}
