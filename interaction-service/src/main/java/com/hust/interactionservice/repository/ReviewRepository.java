package com.hust.interactionservice.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.hust.interactionservice.entity.Review;

@Repository
public interface ReviewRepository extends MongoRepository<Review, String> {
    List<Review> findByCourseIdIn(List<String> courseIds);
    List<Review> findByCourseId(String courseId);
    Long countByCourseId(String courseId);
}
