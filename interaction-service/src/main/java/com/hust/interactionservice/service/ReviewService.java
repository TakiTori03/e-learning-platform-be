package com.hust.interactionservice.service;

import java.util.List;

import org.springframework.data.domain.Pageable;

import com.hust.commonlibrary.dto.ListResponse;
import com.hust.interactionservice.dto.request.ReviewRequest;
import com.hust.interactionservice.dto.response.InternalCourseRatingResponse;
import com.hust.interactionservice.dto.response.RatingResult;
import com.hust.interactionservice.dto.response.ReviewResponse;

public interface ReviewService {
    ReviewResponse createReview(ReviewRequest request);
    ListResponse<ReviewResponse> getReviewsByCourse(String courseId, String query, Integer rating, Pageable pageable);
    ListResponse<ReviewResponse> getAdminReviews(String courseId, String query, Integer rating, Pageable pageable);
    ListResponse<ReviewResponse> getInstructorReviews(String courseId, String query, Integer rating, Pageable pageable);
    RatingResult getCourseRatingSummary(String courseId);

    void deleteReviewByAdmin(String reviewId);
    void deleteReviewReplyByAdmin(String replyId);
    List<InternalCourseRatingResponse> getCourseRatingsBulk(List<String> courseIds);

    // Review Reply
    ReviewResponse createReply(String reviewId, String content);
    List<ReviewResponse> getRepliesByReview(String reviewId);
}
