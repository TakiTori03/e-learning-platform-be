package com.hust.interactionservice.repository;

import com.hust.interactionservice.entity.ReviewReply;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewReplyRepository extends MongoRepository<ReviewReply, String> {
    List<ReviewReply> findByReviewId(String reviewId);
    List<ReviewReply> findByReviewIdIn(java.util.Collection<String> reviewIds);
    void deleteByReviewId(String reviewId);
}
