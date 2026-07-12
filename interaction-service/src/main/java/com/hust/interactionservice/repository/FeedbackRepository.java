package com.hust.interactionservice.repository;

import com.hust.interactionservice.constant.FeedbackStatus;
import com.hust.interactionservice.entity.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedbackRepository extends MongoRepository<Feedback, String> {
    Page<Feedback> findByEmail(String email, Pageable pageable);
    Page<Feedback> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);
    Page<Feedback> findByStatus(FeedbackStatus status, Pageable pageable);
    Page<Feedback> findByTitleContainingIgnoreCaseAndStatus(String keyword, FeedbackStatus status, Pageable pageable);
}
