package com.hust.interactionservice.service.impl;


import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import com.hust.commonlibrary.dto.ListResponse;
import com.hust.commonlibrary.event.FeedbackRepliedEvent;
import com.hust.interactionservice.constant.FeedbackStatus;
import com.hust.interactionservice.constant.FeedbackType;
import com.hust.interactionservice.dto.request.FeedbackReplyRequest;
import com.hust.interactionservice.dto.request.FeedbackRequest;
import com.hust.interactionservice.dto.response.FeedbackReplyResponse;
import com.hust.interactionservice.dto.response.FeedbackResponse;
import com.hust.interactionservice.entity.Feedback;
import com.hust.interactionservice.entity.FeedbackReply;
import com.hust.interactionservice.mapper.FeedbackMapper;
import com.hust.interactionservice.mapper.FeedbackReplyMapper;
import com.hust.interactionservice.repository.FeedbackReplyRepository;
import com.hust.interactionservice.repository.FeedbackRepository;
import com.hust.interactionservice.service.FeedbackService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final FeedbackReplyRepository feedbackReplyRepository;
    private final FeedbackMapper feedbackMapper;
    private final FeedbackReplyMapper feedbackReplyMapper;
    private final MongoTemplate mongoTemplate;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public FeedbackResponse createFeedback(FeedbackRequest request) {
        Feedback feedback = feedbackMapper.requestToEntity(request);
        feedback.setStatus(FeedbackStatus.PENDING);

        Feedback saved = feedbackRepository.save(feedback);
        return feedbackMapper.entityToResponse(saved);
    }



    @Override
    public FeedbackResponse getFeedbackDetail(String id) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Feedback not found"));
        return populateReplies(feedback);
    }

    @Override
    public ListResponse<FeedbackResponse> getAllFeedbacks(String keyword, FeedbackType type, FeedbackStatus status, Pageable pageable) {
        org.springframework.data.mongodb.core.query.Query query = new org.springframework.data.mongodb.core.query.Query();

        if (keyword != null && !keyword.isBlank()) {
            org.springframework.data.mongodb.core.query.Criteria criteria = new org.springframework.data.mongodb.core.query.Criteria().orOperator(
                org.springframework.data.mongodb.core.query.Criteria.where("title").regex(keyword, "i"),
                org.springframework.data.mongodb.core.query.Criteria.where("content").regex(keyword, "i"),
                org.springframework.data.mongodb.core.query.Criteria.where("name").regex(keyword, "i"),
                org.springframework.data.mongodb.core.query.Criteria.where("email").regex(keyword, "i")
            );
            query.addCriteria(criteria);
        }

        if (type != null) {
            query.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where("type").is(type));
        }

        if (status != null) {
            query.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where("status").is(status));
        }

        long total = mongoTemplate.count(query, Feedback.class);
        query.with(pageable);
        List<Feedback> feedbacks = mongoTemplate.find(query, Feedback.class);

        List<FeedbackResponse> content = feedbacks.stream()
                .map(this::populateReplies)
                .toList();

        return ListResponse.of(
            content,
            pageable.getPageNumber() + 1,
            pageable.getPageSize(),
            total,
            (int) Math.ceil((double) total / pageable.getPageSize()),
            (pageable.getOffset() + pageable.getPageSize()) >= total
        );
    }

    @Override
    public FeedbackResponse updateFeedbackStatus(String id, FeedbackStatus status) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Feedback not found"));
        feedback.setStatus(status);
        feedbackRepository.save(feedback);
        return populateReplies(feedback);
    }

    @Override
    public FeedbackReplyResponse replyFeedback(String feedbackId, FeedbackReplyRequest request) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new RuntimeException("Feedback not found"));

        FeedbackReply reply = feedbackReplyMapper.requestToEntity(request);
        reply.setFeedbackId(feedbackId);

        FeedbackReply saved = feedbackReplyRepository.save(reply);

        if (FeedbackStatus.PENDING.equals(feedback.getStatus())) {
            feedback.setStatus(FeedbackStatus.RESPONDED);
            feedbackRepository.save(feedback);
        }

        // Publish FeedbackRepliedEvent to Kafka via ApplicationEventPublisher
        FeedbackRepliedEvent event = FeedbackRepliedEvent.builder()
                .feedbackId(feedbackId)
                .userId(feedback.getCreatedBy())
                .userEmail(feedback.getEmail())
                .userFullName(feedback.getName())
                .title(feedback.getTitle())
                .content(feedback.getContent())
                .replyContent(request.getContent())
                .repliedBy(saved.getCreatedBy())
                .build();
        eventPublisher.publishEvent(event);

        return feedbackReplyMapper.entityToResponse(saved);
    }



    private FeedbackResponse populateReplies(Feedback feedback) {
        FeedbackResponse response = feedbackMapper.entityToResponse(feedback);
        List<FeedbackReplyResponse> replies = feedbackReplyRepository.findByFeedbackId(feedback.getId())
                .stream()
                .map(feedbackReplyMapper::entityToResponse)
                .toList();
        response.setReplies(replies);
        return response;
    }
}
