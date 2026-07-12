package com.hust.learningservice.repository;

import com.hust.learningservice.entity.LessonProgress;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgressRepository extends MongoRepository<LessonProgress, String> {
    List<LessonProgress> findByUserIdAndCourseId(String userId, String courseId);
    Optional<LessonProgress> findByUserIdAndCourseIdAndLessonId(String userId, String courseId, String lessonId);
    List<LessonProgress> findAllByUserIdAndCourseIdIn(String userId, List<String> courseIds);
}
