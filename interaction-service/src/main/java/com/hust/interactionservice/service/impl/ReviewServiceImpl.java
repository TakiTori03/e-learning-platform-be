package com.hust.interactionservice.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.hust.commonlibrary.annotation.CheckCourseOwner;
import com.hust.commonlibrary.constants.RedisPrefixConstants;
import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.commonlibrary.dto.CourseInternalResponse;
import com.hust.commonlibrary.dto.ListResponse;
import com.hust.commonlibrary.dto.UserSharedProfile;
import com.hust.commonlibrary.event.CourseReviewUpdatedEvent;
import com.hust.commonlibrary.service.RedisService;
import com.hust.commonlibrary.utils.SecurityUtils;
import com.hust.interactionservice.client.CourseClient;
import com.hust.interactionservice.dto.request.ReviewRequest;
import com.hust.interactionservice.dto.response.RatingResult;
import com.hust.interactionservice.dto.response.ReviewResponse;
import com.hust.interactionservice.entity.Review;
import com.hust.interactionservice.entity.ReviewReply;
import com.hust.interactionservice.mapper.ReviewMapper;
import com.hust.interactionservice.mapper.ReviewReplyMapper;
import com.hust.interactionservice.repository.ReviewReplyRepository;
import com.hust.interactionservice.repository.ReviewRepository;
import com.hust.interactionservice.service.ReviewService;
import com.hust.interactionservice.utils.AppUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewReplyRepository reviewReplyRepository;
    private final ReviewMapper reviewMapper;
    private final ReviewReplyMapper reviewReplyMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final RedisService redisService;
    private final CourseClient courseClient;
    private final MongoTemplate mongoTemplate;

    @Override
    public ReviewResponse createReview(ReviewRequest request) {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        Review review = reviewMapper.requestToEntity(request);
        review.setUserId(userId);
        review.setCreatedBy(userId);
        review.setCode(AppUtils.generateCode("REV"));

        Review savedReview = reviewRepository.save(review);
        publishReviewStats(request.getCourseId());

        ReviewResponse response = reviewMapper.entityToResponse(savedReview);
        enrichProfiles(java.util.Collections.singletonList(response));
        return response;
    }

    @Override
    public ListResponse<ReviewResponse> getReviewsByCourse(String courseId, String query, Integer rating, Pageable pageable) {
        Page<Review> reviewPageRaw = executeSearchQuery(courseId, query, rating, pageable);
        return buildReviewListResponse(reviewPageRaw);
    }

    @Override
    public ListResponse<ReviewResponse> getAdminReviews(String courseId, String query, Integer rating, Pageable pageable) {
        Page<Review> reviewPageRaw = executeSearchQuery(courseId, query, rating, pageable);
        return buildReviewListResponse(reviewPageRaw);
    }

    private Page<Review> executeSearchQuery(String courseId, String query, Integer rating, Pageable pageable) {
        Query dbQuery = new Query();

        if (courseId != null && !courseId.isBlank()) {
            dbQuery.addCriteria(Criteria.where("courseId").is(courseId));
        }

        if (query != null && !query.isBlank()) {
            dbQuery.addCriteria(Criteria.where("title").regex(Pattern.quote(query.trim()), "i"));
        }

        if (rating != null) {
            List<Double> stars = switch (rating) {
                case 1 -> List.of(0.5, 1.0, 1.5);
                case 2 -> List.of(2.0, 2.5);
                case 3 -> List.of(3.0, 3.5);
                case 4 -> List.of(4.0, 4.5);
                case 5 -> List.of(5.0);
                default -> null;
            };
            if (stars != null) {
                dbQuery.addCriteria(Criteria.where("ratingStar").in(stars));
            }
        }

        long total = mongoTemplate.count(dbQuery, Review.class);
        dbQuery.with(pageable);
        List<Review> reviews = mongoTemplate.find(dbQuery, Review.class);

        return new PageImpl<>(reviews, pageable, total);
    }

    private ListResponse<ReviewResponse> buildReviewListResponse(Page<Review> reviewPageRaw) {
        List<ReviewResponse> reviewResponses = reviewPageRaw.getContent().stream()
                .map(reviewMapper::entityToResponse)
                .toList();

        // Populate Replies
        List<String> reviewIds = reviewResponses.stream().map(ReviewResponse::getId).toList();
        List<ReviewReply> allReplies = reviewReplyRepository.findByReviewIdIn(reviewIds);

        Map<String, List<ReviewReply>> groupedReplies = allReplies.stream()
                .collect(Collectors.groupingBy(ReviewReply::getReviewId));

        reviewResponses.forEach(res -> {
            List<ReviewReply> replies = groupedReplies.getOrDefault(res.getId(), List.of());
            res.setReplies(replies.stream().map(reviewReplyMapper::entityToResponse).toList());
        });

        enrichProfiles(reviewResponses);
        enrichCourses(reviewResponses);

        return ListResponse.of(
                reviewResponses,
                reviewPageRaw.getNumber() + 1,
                reviewPageRaw.getSize(),
                reviewPageRaw.getTotalElements(),
                reviewPageRaw.getTotalPages(),
                reviewPageRaw.isLast()
        );
    }

    @Override
    public ListResponse<ReviewResponse> getInstructorReviews(String courseId, String query, Integer rating, Pageable pageable) {
        if (courseId == null || courseId.isBlank()) {
            return ListResponse.of(List.of(), 1, pageable.getPageSize(), 0, 0, true);
        }
        return getReviewsByCourse(courseId, query, rating, pageable);
    }

    @Override
    @CheckCourseOwner(domainId = "#reviewId", resolver = "reviewResolver")
    public ReviewResponse createReply(String reviewId, String content) {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        ReviewReply reply = ReviewReply.builder()
                .reviewId(reviewId)
                .userId(userId)
                .content(content)
                .code(AppUtils.generateCode("REPLY"))
                .createdBy(userId)
                .build();

        ReviewResponse response = reviewReplyMapper.entityToResponse(reviewReplyRepository.save(reply));
        enrichProfiles(java.util.Collections.singletonList(response));
        return response;
    }

    @Override
    public List<ReviewResponse> getRepliesByReview(String reviewId) {
        List<ReviewResponse> responses = reviewReplyRepository.findByReviewId(reviewId).stream()
                .map(reviewReplyMapper::entityToResponse)
                .toList();
        enrichProfiles(responses);
        return responses;
    }

    @Override
    public void deleteReviewByAdmin(String reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new com.hust.commonlibrary.exception.payload.ResourceNotFoundException("Review", "id", reviewId));
        String courseId = review.getCourseId();
        
        reviewRepository.delete(review);
        reviewReplyRepository.deleteByReviewId(reviewId);
        
        publishReviewStats(courseId);
    }

    @Override
    public void deleteReviewReplyByAdmin(String replyId) {
        reviewReplyRepository.deleteById(replyId);
    }

    @Override
    public List<com.hust.interactionservice.dto.response.InternalCourseRatingResponse> getCourseRatingsBulk(List<String> courseIds) {
        List<Review> allReviews = reviewRepository.findByCourseIdIn(courseIds);

        Map<String, List<Review>> grouped = allReviews.stream()
                .collect(Collectors.groupingBy(Review::getCourseId));

        return courseIds.stream().map(cid -> {
            List<Review> courseReviews = grouped.getOrDefault(cid, List.of());
            long count = courseReviews.size();
            double avg = count > 0
                    ? courseReviews.stream().mapToDouble(Review::getRatingStar).average().orElse(0.0)
                    : 0.0;

            return com.hust.interactionservice.dto.response.InternalCourseRatingResponse.builder()
                    .courseId(cid)
                    .avgRatingStars(Math.round(avg * 10.0) / 10.0)
                    .numOfReviews(count)
                    .build();
        }).toList();
    }

    @Override
    public RatingResult getCourseRatingSummary(String courseId) {
        List<Review> reviews = reviewRepository.findByCourseId(courseId);
        long total = reviews.size();

        if (total == 0) {
            return RatingResult.builder()
                    .averageRating(0.0)
                    .totalReviews(0L)
                    .ratingPercentages(new HashMap<>())
                    .build();
        }

        double sum = reviews.stream().mapToDouble(Review::getRatingStar).sum();
        double average = sum / total;

        // Tính toán phần trăm theo nhóm (Copy logic từ Monolith)
        Map<String, Long> counts = new HashMap<>();
        for (Review r : reviews) {
            double star = r.getRatingStar();
            String group = "5";
            if (star < 2.0) group = "1";
            else if (star < 3.0) group = "2";
            else if (star < 4.0) group = "3";
            else if (star < 5.0) group = "4";

            counts.put(group, counts.getOrDefault(group, 0L) + 1);
        }

        Map<String, String> percentages = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            String key = String.valueOf(i);
            long count = counts.getOrDefault(key, 0L);
            percentages.put(key, String.format("%.1f%%", (count * 100.0) / total));
        }

        return RatingResult.builder()
                .averageRating(Math.round(average * 10.0) / 10.0) // Làm tròn 1 chữ số thập phân
                .totalReviews(total)
                .ratingPercentages(percentages)
                .build();
    }

    /**
     * Tính toán gộp (Aggregation) thống kê đánh giá của khóa học rồi phát sự kiện Spring local.
     * ReviewEventListener sẽ lắng nghe sự kiện này và gửi lên Kafka.
     */
    private void publishReviewStats(String courseId) {
        try {
            List<Review> reviews = reviewRepository.findByCourseId(courseId);
            long count = reviews.size();
            double avg = count > 0
                    ? Math.round(reviews.stream().mapToDouble(Review::getRatingStar).average().orElse(0.0) * 10.0) / 10.0
                    : 0.0;

            CourseReviewUpdatedEvent event = CourseReviewUpdatedEvent.builder()
                    .courseId(courseId)
                    .avgRatingStars(avg)
                    .numOfReviews(count)
                    .build();

            eventPublisher.publishEvent(event);
            log.info("📤 Published local CourseReviewUpdatedEvent: courseId={}, avg={}, count={}", courseId, avg, count);
        } catch (Exception e) {
            log.error("⚠️ Failed to publish local review stats for course {}: {}", courseId, e.getMessage());
        }
    }

    private void enrichProfiles(List<ReviewResponse> responses) {
        if (responses == null || responses.isEmpty()) return;

        List<String> userIds = new java.util.ArrayList<>();
        collectUserIds(responses, userIds);

        List<String> distinctUserIds = userIds.stream().distinct().toList();
        List<String> redisKeys = distinctUserIds.stream()
                .map(RedisPrefixConstants::getSharedUserProfileKey)
                .toList();

        try {
            List<Object> cachedProfiles = redisService.multiGet(redisKeys);
            Map<String, UserSharedProfile> profileMap = new java.util.HashMap<>();

            for (int i = 0; i < distinctUserIds.size(); i++) {
                String uId = distinctUserIds.get(i);
                Object val = cachedProfiles != null && i < cachedProfiles.size() ? cachedProfiles.get(i) : null;
                if (val != null) {
                    if (val instanceof UserSharedProfile profile) {
                        profileMap.put(uId, profile);
                    } else {
                        UserSharedProfile profile = new com.fasterxml.jackson.databind.ObjectMapper()
                                .convertValue(val, UserSharedProfile.class);
                        profileMap.put(uId, profile);
                    }
                }
            }

            populateProfiles(responses, profileMap);
        } catch (Exception e) {
            log.error("Failed to enrich review user profiles from Redis: {}", e.getMessage(), e);
        }
    }

    private void collectUserIds(List<ReviewResponse> responses, List<String> userIds) {
        for (ReviewResponse res : responses) {
            if (res.getUserId() != null) {
                userIds.add(res.getUserId());
            }
            if (res.getReplies() != null && !res.getReplies().isEmpty()) {
                collectUserIds(res.getReplies(), userIds);
            }
        }
    }

    private void populateProfiles(List<ReviewResponse> responses, Map<String, UserSharedProfile> profileMap) {
        for (ReviewResponse res : responses) {
            UserSharedProfile profile = profileMap.get(res.getUserId());
            if (profile != null) {
                res.setUser(profile);
            }
            if (res.getReplies() != null && !res.getReplies().isEmpty()) {
                populateProfiles(res.getReplies(), profileMap);
            }
        }
    }

    private void enrichCourses(List<ReviewResponse> responses) {
        if (responses == null || responses.isEmpty()) return;
        List<String> courseIds = responses.stream()
                .map(ReviewResponse::getCourseId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        if (courseIds.isEmpty()) return;

        try {
            ApiResponse<List<CourseInternalResponse>> coursesRes = courseClient.getBulkCourses(courseIds);
            List<CourseInternalResponse> courses = coursesRes.getPayload() != null ? coursesRes.getPayload() : List.of();
            Map<String, CourseInternalResponse> courseMap = courses.stream()
                    .collect(Collectors.toMap(CourseInternalResponse::getId, c -> c));

            for (ReviewResponse res : responses) {
                CourseInternalResponse course = courseMap.get(res.getCourseId());
                if (course != null) {
                    res.setCourse(ReviewResponse.CourseDto.builder()
                            .id(course.getId())
                            .name(course.getName())
                            .thumbnail(course.getThumbnail())
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("Failed to enrich courses for reviews: {}", e.getMessage());
        }
    }
}
