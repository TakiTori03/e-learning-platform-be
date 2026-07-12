package com.hust.assessmentservice.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.hust.assessmentservice.entity.QuizAttempt;

@Repository
public interface QuizAttemptRepository extends MongoRepository<QuizAttempt, String> {
    List<QuizAttempt> findAllByUserIdAndQuizIdOrderBySubmittedAtDesc(String userId, String quizId);

    long countByUserIdAndQuizId(String userId, String quizId);

    List<QuizAttempt> findAllByUserIdAndCourseIdOrderBySubmittedAtDesc(String userId, String courseId);

    List<QuizAttempt> findAllByUserIdAndCourseIdInOrderBySubmittedAtDesc(String userId, List<String> courseIds);
}
