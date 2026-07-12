package com.hust.interactionservice.repository;

import com.hust.interactionservice.entity.FeedbackReply;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackReplyRepository extends MongoRepository<FeedbackReply, String> {
    List<FeedbackReply> findByFeedbackId(String feedbackId);
}
