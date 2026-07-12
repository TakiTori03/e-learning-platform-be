package com.hust.assessmentservice.repository;

import com.hust.assessmentservice.entity.Quiz;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizRepository extends MongoRepository<Quiz, String> {
    Optional<Quiz> findByLessonId(String lessonId);
    Optional<Quiz> findByCourseIdAndType(String courseId, com.hust.commonlibrary.entity.QuizType type);
    List<Quiz> findAllByCourseIdIn(List<String> courseIds);

}
