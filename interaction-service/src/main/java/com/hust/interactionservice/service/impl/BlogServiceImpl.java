package com.hust.interactionservice.service.impl;

import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hust.commonlibrary.constants.AppConstants;
import com.hust.commonlibrary.constants.RedisPrefixConstants;
import com.hust.commonlibrary.dto.ListResponse;
import com.hust.commonlibrary.dto.UserSharedProfile;
import com.hust.commonlibrary.exception.AppException;
import com.hust.commonlibrary.exception.ErrorCode;
import com.hust.commonlibrary.exception.payload.InvalidParamException;
import com.hust.commonlibrary.exception.payload.ResourceNotFoundException;
import com.hust.commonlibrary.service.RedisService;
import com.hust.commonlibrary.utils.SecurityUtils;
import com.hust.interactionservice.dto.request.BlogCommentRequest;
import com.hust.interactionservice.dto.request.BlogPostRequest;
import com.hust.interactionservice.dto.request.BlogTopicRequest;
import com.hust.interactionservice.dto.response.BlogAdminStatsResponse;
import com.hust.interactionservice.dto.response.BlogCommentResponse;
import com.hust.interactionservice.dto.response.BlogInstructorStatsResponse;
import com.hust.interactionservice.dto.response.BlogPostResponse;
import com.hust.interactionservice.dto.response.BlogTopicResponse;
import com.hust.interactionservice.entity.BlogComment;
import com.hust.interactionservice.entity.BlogPost;
import com.hust.interactionservice.entity.BlogPost.ReportInfo;
import com.hust.interactionservice.entity.BlogTopic;
import com.hust.interactionservice.entity.enums.BlogStatus;
import com.hust.interactionservice.mapper.BlogCommentMapper;
import com.hust.interactionservice.mapper.BlogPostMapper;
import com.hust.interactionservice.mapper.BlogTopicMapper;
import com.hust.interactionservice.repository.BlogCommentRepository;
import com.hust.interactionservice.repository.BlogPostRepository;
import com.hust.interactionservice.repository.BlogTopicRepository;
import com.hust.interactionservice.service.BlogService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlogServiceImpl implements BlogService {

    private final BlogTopicRepository blogTopicRepository;
    private final BlogTopicMapper blogTopicMapper;

    private final BlogPostRepository blogPostRepository;
    private final BlogPostMapper blogPostMapper;

    private final BlogCommentRepository blogCommentRepository;
    private final BlogCommentMapper blogCommentMapper;

    private final RedisService redisService;
    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private BlogTopicResponse toResponseWithCount(BlogTopic entity) {
        BlogTopicResponse response = blogTopicMapper.entityToResponse(entity);
        if (response != null) {
            long count = mongoTemplate.count(
                    new Query(Criteria.where("topic.id").is(entity.getId())),
                    BlogPost.class
            );
            response.setPostCount(count);
        }
        return response;
    }

    private List<BlogTopicResponse> toResponseWithCount(List<BlogTopic> entities) {
        List<BlogTopicResponse> responses = blogTopicMapper.entityToResponse(entities);
        if (responses != null && !responses.isEmpty()) {
            List<String> ids = responses.stream().map(BlogTopicResponse::getId).toList();
            org.springframework.data.mongodb.core.aggregation.Aggregation aggregation =
                org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation(
                    org.springframework.data.mongodb.core.aggregation.Aggregation.match(
                        Criteria.where("topic.id").in(ids)
                    ),
                    org.springframework.data.mongodb.core.aggregation.Aggregation.group("topic.id").count().as("count")
                );

            org.springframework.data.mongodb.core.aggregation.AggregationResults<org.bson.Document> results =
                mongoTemplate.aggregate(aggregation, BlogPost.class, org.bson.Document.class);

            Map<String, Long> countMap = new HashMap<>();
            for (org.bson.Document row : results.getMappedResults()) {
                Object idObj = row.get("_id");
                Object countObj = row.get("count");
                if (idObj != null && countObj != null) {
                    countMap.put(idObj.toString(), ((Number) countObj).longValue());
                }
            }

            for (BlogTopicResponse res : responses) {
                res.setPostCount(countMap.getOrDefault(res.getId(), 0L));
            }
        }
        return responses;
    }

    // === TOPIC MANAGEMENT ===
    @Override
    public List<BlogTopicResponse> getAllTopics() {
        List<BlogTopic> list = blogTopicRepository.findAll();
        return toResponseWithCount(list);
    }

    @Override
    public ListResponse<BlogTopicResponse> searchTopics(String text, Pageable pageable) {
        Query query = new Query();

        if (text != null && !text.trim().isEmpty()) {
            String sanitizedText = Pattern.quote(text.trim());
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("name").regex(sanitizedText, "i"),
                    Criteria.where("description").regex(sanitizedText, "i")
            ));
        }

        long total = mongoTemplate.count(query, BlogTopic.class);
        query.with(pageable);
        List<BlogTopic> topics = mongoTemplate.find(query, BlogTopic.class);

        List<BlogTopicResponse> responseList = toResponseWithCount(topics);

        Page<BlogTopic> topicPage = new org.springframework.data.domain.PageImpl<>(topics, pageable, total);
        return ListResponse.of(responseList, topicPage);
    }

    @Override
    public BlogTopicResponse getTopicDetail(String id) {
        BlogTopic entity = blogTopicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AppConstants.Resource_Constants.TOPIC, AppConstants.Field_Constants.ID, id));
        return toResponseWithCount(entity);
    }

    @Override
    public BlogTopicResponse createTopic(BlogTopicRequest request) {
        String slug = toSlug(request.getName());
        if (blogTopicRepository.existsBySlug(slug)) {
            throw new InvalidParamException("Slug của chủ đề đã tồn tại!");
        }
        if (blogTopicRepository.existsByName(request.getName())) {
            throw new InvalidParamException("Tên chủ đề đã tồn tại!");
        }
        BlogTopic entity = blogTopicMapper.requestToEntity(request);
        entity.setSlug(slug);
        BlogTopic saved = blogTopicRepository.save(entity);
        log.info("Created new blog topic: name={}, slug={}", saved.getName(), saved.getSlug());
        return toResponseWithCount(saved);
    }

    @Override
    public BlogTopicResponse updateTopic(String id, BlogTopicRequest request) {
        BlogTopic entity = blogTopicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AppConstants.Resource_Constants.TOPIC, AppConstants.Field_Constants.ID, id));

        String slug = toSlug(request.getName());

        if (!entity.getName().equalsIgnoreCase(request.getName()) && blogTopicRepository.existsByName(request.getName())) {
            throw new InvalidParamException("Tên chủ đề đã tồn tại!");
        }
        if (!entity.getSlug().equalsIgnoreCase(slug) && blogTopicRepository.existsBySlug(slug)) {
            throw new InvalidParamException("Slug của chủ đề đã tồn tại!");
        }

        blogTopicMapper.partialUpdate(entity, request);
        entity.setSlug(slug);
        BlogTopic saved = blogTopicRepository.save(entity);
        log.info("Updated blog topic: id={}, name={}", saved.getId(), saved.getName());
        return toResponseWithCount(saved);
    }

    @Override
    public void deleteTopic(String id) {
        BlogTopic entity = blogTopicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AppConstants.Resource_Constants.TOPIC, AppConstants.Field_Constants.ID, id));

        // Check if any blog post is associated with this topic
        long count = mongoTemplate.count(new Query(Criteria.where("topic.id").is(id)), BlogPost.class);
        if (count > 0) {
            throw new InvalidParamException("Không thể xóa chủ đề vì đang có " + count + " bài viết liên kết!");
        }

        blogTopicRepository.delete(entity);
        log.info("Deleted blog topic: id={}, name={}", entity.getId(), entity.getName());
    }

    // === BLOG POST MANAGEMENT ===
    @Override
    public BlogPostResponse createBlogPost(BlogPostRequest request) {
        String slug = toSlug(request.getTitle());
        if (blogPostRepository.existsBySlug(slug)) {
            // Append random string to slug if duplicate
            slug = slug + "-" + UUID.randomUUID().toString().substring(0, 8);
        }
        BlogTopic topic = blogTopicRepository.findById(request.getTopicId())
                .orElseThrow(() -> new ResourceNotFoundException(AppConstants.Resource_Constants.TOPIC, AppConstants.Field_Constants.ID, request.getTopicId()));

        BlogPost blogPost = blogPostMapper.requestToEntity(request);
        blogPost.setSlug(slug);
        blogPost.setStatus(BlogStatus.DRAFT);
        blogPost.setIsPinned(false);
        blogPost.setTopic(topic);

        BlogPost saved = blogPostRepository.save(blogPost);
        log.info("Created draft blog post: id={}, title={}", saved.getId(), saved.getTitle());
        return mapToResponse(saved);
    }

    @Override
    public BlogPostResponse updateBlogPost(String id, BlogPostRequest request) {
        BlogPost blogPost = blogPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AppConstants.Resource_Constants.TOPIC, AppConstants.Field_Constants.ID, id));

        String currentUserId = SecurityUtils.getCurrentUserIdOrThrow();
        if (!currentUserId.equals(blogPost.getCreatedBy())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        String slug = toSlug(request.getTitle());
        if (!blogPost.getSlug().equalsIgnoreCase(slug) && blogPostRepository.existsBySlug(slug)) {
            // Append random string to slug if duplicate
            slug = slug + "-" + UUID.randomUUID().toString().substring(0, 8);
        }

        BlogTopic topic = blogTopicRepository.findById(request.getTopicId())
                .orElseThrow(() -> new ResourceNotFoundException(AppConstants.Resource_Constants.TOPIC, AppConstants.Field_Constants.ID, request.getTopicId()));

        blogPostMapper.partialUpdate(blogPost, request);
        blogPost.setSlug(slug);
        blogPost.setTopic(topic);

        BlogPost saved = blogPostRepository.save(blogPost);
        log.info("Updated blog post: id={}", saved.getId());
        return mapToResponse(saved);
    }

    @Override
    public void deleteBlogPost(String id) {
        BlogPost blogPost = blogPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AppConstants.Resource_Constants.BLOG, AppConstants.Field_Constants.ID, id));

        String currentUserId = SecurityUtils.getCurrentUserIdOrThrow();
        if (!currentUserId.equals(blogPost.getCreatedBy())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        blogPostRepository.delete(blogPost);
        blogCommentRepository.deleteByBlogId(id);
        log.info("Deleted blog post: id={}", id);
    }

    @Override
    public BlogPostResponse publishBlogPost(String id) {
        BlogPost blogPost = blogPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AppConstants.Resource_Constants.BLOG, AppConstants.Field_Constants.ID, id));

        String currentUserId = SecurityUtils.getCurrentUserIdOrThrow();
        if (!currentUserId.equals(blogPost.getCreatedBy())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        blogPost.setStatus(BlogStatus.PUBLISHED);
        BlogPost saved = blogPostRepository.save(blogPost);
        log.info("Published blog post: id={}", saved.getId());
        return mapToResponse(saved);
    }

    @Override
    public BlogPostResponse pinBlogPost(String id, Boolean pin) {
        BlogPost blogPost = blogPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AppConstants.Resource_Constants.BLOG, AppConstants.Field_Constants.ID, id));

        blogPost.setIsPinned(pin != null && pin);
        BlogPost saved = blogPostRepository.save(blogPost);
        log.info("Pinned/Unpinned blog post: id={}, isPinned={}", saved.getId(), saved.getIsPinned());
        return mapToResponse(saved);
    }

    @Override
    public BlogPostResponse blockBlogPost(String id, Boolean block) {
        BlogPost blogPost = blogPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AppConstants.Resource_Constants.BLOG, AppConstants.Field_Constants.ID, id));

        if (block != null && block) {
            blogPost.setStatus(BlogStatus.BLOCKED);
        } else {
            blogPost.setStatus(BlogStatus.PUBLISHED); // Unblock returns it to published
        }

        BlogPost saved = blogPostRepository.save(blogPost);
        log.info("Blocked/Unblocked blog post: id={}, status={}", saved.getId(), saved.getStatus());
        return mapToResponse(saved);
    }

    @Override
    public BlogPostResponse getBlogPostDetail(String slug) {
        BlogPost blogPost = blogPostRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException(AppConstants.Resource_Constants.BLOG, AppConstants.Field_Constants.SLUG, slug));

        // If not published, only author or admin can view
        if (blogPost.getStatus() != BlogStatus.PUBLISHED) {
            Optional<String> currentUserIdOpt = SecurityUtils.getCurrentUserId();
            if (currentUserIdOpt.isEmpty()) {
                throw new AppException(ErrorCode.UNAUTHORIZED);
            }
            String currentUserId = currentUserIdOpt.get();
            if (!currentUserId.equals(blogPost.getCreatedBy()) && !SecurityUtils.isAdmin()) {
                throw new AppException(ErrorCode.UNAUTHORIZED);
            }
        }

        // Increment view count
        blogPost.setViewsCount(blogPost.getViewsCount() + 1);
        blogPostRepository.save(blogPost);

        BlogPostResponse response = mapToResponse(blogPost);
        enrichUserProfiles(Collections.singletonList(response));
        return response;
    }

    @Override
    public ListResponse<BlogPostResponse> getPublishedBlogPosts(String topicId, String search, String tag, String authorId, List<String> excludeIds, Pageable pageable) {
        Query query = new Query();
        query.addCriteria(Criteria.where("status").is(BlogStatus.PUBLISHED));

        if (excludeIds != null && !excludeIds.isEmpty()) {
            query.addCriteria(Criteria.where("id").nin(excludeIds));
        }

        if (StringUtils.hasText(topicId)) {
            query.addCriteria(Criteria.where("topic.id").is(topicId));
        }

        if (StringUtils.hasText(search)) {
            query.addCriteria(Criteria.where("title").regex(search, "i"));
        }

        if (StringUtils.hasText(tag)) {
            query.addCriteria(Criteria.where("tags").is(tag));
        }

        if (StringUtils.hasText(authorId)) {
            query.addCriteria(Criteria.where("createdBy").is(authorId));
        }

        long total = mongoTemplate.count(query, BlogPost.class);
        query.with(pageable);
        List<BlogPost> posts = mongoTemplate.find(query, BlogPost.class);

        List<BlogPostResponse> responses = posts.stream()
                .map(this::mapToResponse)
                .toList();

        enrichUserProfiles(responses);

        int totalPages = (int) Math.ceil((double) total / pageable.getPageSize());
        boolean isLast = pageable.getOffset() + pageable.getPageSize() >= total;

        return ListResponse.of(
                responses,
                pageable.getPageNumber() + 1,
                pageable.getPageSize(),
                total,
                totalPages,
                isLast
        );
    }

    @Override
    public List<BlogPostResponse> getFeaturedBlogPosts(int size) {
        // Query for pinned posts
        Query pinnedQuery = new Query();
        pinnedQuery.addCriteria(Criteria.where("status").is(BlogStatus.PUBLISHED));
        pinnedQuery.addCriteria(Criteria.where("isPinned").is(true));
        pinnedQuery.with(Sort.by(Sort.Direction.DESC, "createdAt"));
        pinnedQuery.limit(size);
        List<BlogPost> pinnedPosts = mongoTemplate.find(pinnedQuery, BlogPost.class);

        List<BlogPost> result = new ArrayList<>(pinnedPosts);

        // If we need more posts to fill the size, fallback to newest published posts
        if (result.size() < size) {
            int remaining = size - result.size();
            List<String> pinnedIds = pinnedPosts.stream().map(BlogPost::getId).toList();

            Query fallbackQuery = new Query();
            fallbackQuery.addCriteria(Criteria.where("status").is(BlogStatus.PUBLISHED));
            if (!pinnedIds.isEmpty()) {
                fallbackQuery.addCriteria(Criteria.where("id").nin(pinnedIds));
            }
            fallbackQuery.with(Sort.by(Sort.Direction.DESC, "createdAt"));
            fallbackQuery.limit(remaining);
            List<BlogPost> fallbackPosts = mongoTemplate.find(fallbackQuery, BlogPost.class);
            result.addAll(fallbackPosts);
        }

        List<BlogPostResponse> responses = result.stream()
                .map(this::mapToResponse)
                .toList();

        enrichUserProfiles(responses);
        return responses;
    }

    @Override
    public ListResponse<BlogPostResponse> getMyBlogPosts(String topicId, BlogStatus status, String search, Pageable pageable) {
        String currentUserId = SecurityUtils.getCurrentUserIdOrThrow();
        Query query = new Query();
        query.addCriteria(Criteria.where("createdBy").is(currentUserId));

        if (status != null) {
            query.addCriteria(Criteria.where("status").is(status));
        }
        if (StringUtils.hasText(topicId)) {
            query.addCriteria(Criteria.where("topic.id").is(topicId));
        }
        if (StringUtils.hasText(search)) {
            query.addCriteria(Criteria.where("title").regex(search, "i"));
        }

        long total = mongoTemplate.count(query, BlogPost.class);
        query.with(pageable);
        List<BlogPost> posts = mongoTemplate.find(query, BlogPost.class);

        List<BlogPostResponse> responses = posts.stream()
                .map(this::mapToResponse)
                .toList();

        enrichUserProfiles(responses);

        int totalPages = (int) Math.ceil((double) total / pageable.getPageSize());
        boolean isLast = pageable.getOffset() + pageable.getPageSize() >= total;

        return ListResponse.of(
                responses,
                pageable.getPageNumber() + 1,
                pageable.getPageSize(),
                total,
                totalPages,
                isLast
        );
    }

    @Override
    public ListResponse<BlogPostResponse> getAdminManagedBlogPosts(String topicId, BlogStatus status, String authorId, Boolean isPinned, Boolean hasReports, String search, Pageable pageable) {
        Query query = new Query();

        if (StringUtils.hasText(topicId)) {
            query.addCriteria(Criteria.where("topic.id").is(topicId));
        }
        if (status != null) {
            query.addCriteria(Criteria.where("status").is(status));
        }
        if (StringUtils.hasText(authorId)) {
            query.addCriteria(Criteria.where("createdBy").is(authorId));
        }
        if (isPinned != null) {
            if (isPinned) {
                query.addCriteria(Criteria.where("isPinned").is(true));
            } else {
                query.addCriteria(new Criteria().orOperator(
                    Criteria.where("isPinned").ne(true),
                    Criteria.where("isPinned").exists(false)
                ));
            }
        }
        if (hasReports != null) {
            if (hasReports) {
                query.addCriteria(Criteria.where("reports.0").exists(true));
            } else {
                query.addCriteria(new Criteria().orOperator(
                    Criteria.where("reports").exists(false),
                    Criteria.where("reports").is(Collections.emptyList()),
                    Criteria.where("reports.0").exists(false)
                ));
            }
        }
        if (StringUtils.hasText(search)) {
            query.addCriteria(Criteria.where("title").regex(search, "i"));
        }

        long total = mongoTemplate.count(query, BlogPost.class);
        query.with(pageable);
        List<BlogPost> posts = mongoTemplate.find(query, BlogPost.class);

        List<BlogPostResponse> responses = posts.stream()
                .map(this::mapToResponse)
                .toList();

        enrichUserProfiles(responses);

        int totalPages = (int) Math.ceil((double) total / pageable.getPageSize());
        boolean isLast = pageable.getOffset() + pageable.getPageSize() >= total;

        return ListResponse.of(
                responses,
                pageable.getPageNumber() + 1,
                pageable.getPageSize(),
                total,
                totalPages,
                isLast
        );
    }

    // === INTERACTIONS (LIKE, REPORT & COMMENTS) ===
    @Override
    public void toggleLikeBlogPost(String id) {
        String currentUserId = SecurityUtils.getCurrentUserIdOrThrow();
        BlogPost blogPost = blogPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AppConstants.Resource_Constants.BLOG, AppConstants.Field_Constants.ID, id));

        if (blogPost.getLikes().contains(currentUserId)) {
            blogPost.getLikes().remove(currentUserId);
        } else {
            blogPost.getLikes().add(currentUserId);
        }

        blogPostRepository.save(blogPost);
    }

    @Override
    public void shareBlogPost(String id) {
        Query query = new Query(Criteria.where("id").is(id));
        Update update = new Update().inc("sharesCount", 1);
        mongoTemplate.updateFirst(query, update, BlogPost.class);
    }

    @Override
    public void reportBlogPost(String id, String reason) {
        String currentUserId = SecurityUtils.getCurrentUserIdOrThrow();
        BlogPost blogPost = blogPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AppConstants.Resource_Constants.BLOG, AppConstants.Field_Constants.ID, id));

        // Avoid duplicate reports from the same user
        boolean alreadyReported = blogPost.getReports().stream()
                .anyMatch(r -> currentUserId.equals(r.getUserId()));

        if (!alreadyReported) {
            blogPost.getReports().add(ReportInfo.builder()
                    .userId(currentUserId)
                    .reason(reason)
                    .createdAt(Instant.now())
                    .build());
            blogPostRepository.save(blogPost);
            log.info("User {} reported blog post {}: reason={}", currentUserId, id, reason);
        }
    }

    @Override
    public BlogCommentResponse createComment(String blogId, BlogCommentRequest request) {
        BlogPost blogPost = blogPostRepository.findById(blogId)
                .orElseThrow(() -> new ResourceNotFoundException(AppConstants.Resource_Constants.BLOG, AppConstants.Field_Constants.ID, blogId));

        // If parentCommentId is provided, verify it exists
        if (StringUtils.hasText(request.getParentCommentId())) {
            BlogComment parent = blogCommentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new ResourceNotFoundException(AppConstants.Resource_Constants.COMMENT, AppConstants.Field_Constants.ID, request.getParentCommentId()));
            if (!blogId.equals(parent.getBlogId())) {
                throw new InvalidParamException("Parent comment does not belong to this blog post");
            }
        }

        BlogComment comment = blogCommentMapper.requestToEntity(request);
        comment.setBlogId(blogId);

        BlogComment saved = blogCommentRepository.save(comment);

        // Update commentsCount in BlogPost
        blogPost.setCommentsCount(blogCommentRepository.countByBlogId(blogId));
        blogPostRepository.save(blogPost);

        log.info("Created comment: id={}, blogId={}", saved.getId(), blogId);

        BlogCommentResponse response = blogCommentMapper.entityToResponse(saved);
        enrichUserProfilesForComments(Collections.singletonList(response));
        return response;
    }

    @Override
    public ListResponse<BlogCommentResponse> getComments(String blogId, Pageable pageable) {
        // Step 1: Query top-level comments
        Page<BlogComment> pageRaw = blogCommentRepository.findByBlogIdAndParentCommentIdIsNull(blogId, pageable);
        List<BlogCommentResponse> comments = pageRaw.getContent().stream()
                .map(blogCommentMapper::entityToResponse)
                .toList();

        if (!comments.isEmpty()) {
            // Step 2: Query replies
            List<String> topCommentIds = comments.stream().map(BlogCommentResponse::getId).toList();
            List<BlogComment> allReplies = blogCommentRepository.findByParentCommentIdIn(topCommentIds);

            // Group replies by parentCommentId
            Map<String, List<BlogComment>> repliesMap = allReplies.stream()
                    .collect(Collectors.groupingBy(BlogComment::getParentCommentId));

            // Map replies to parent DTOs
            comments.forEach(c -> {
                List<BlogComment> replies = repliesMap.getOrDefault(c.getId(), List.of());
                List<BlogCommentResponse> replyResponses = replies.stream()
                        .map(blogCommentMapper::entityToResponse)
                        .sorted(Comparator.comparing(BlogCommentResponse::getCreatedAt))
                        .toList();
                c.setReplies(replyResponses);
            });
        }

        // Enrich profiles for both comments and replies
        List<BlogCommentResponse> allToEnrich = new ArrayList<>(comments);
        comments.forEach(c -> {
            if (c.getReplies() != null) {
                allToEnrich.addAll(c.getReplies());
            }
        });
        enrichUserProfilesForComments(allToEnrich);

        return ListResponse.of(
                comments,
                pageRaw.getNumber() + 1,
                pageRaw.getSize(),
                pageRaw.getTotalElements(),
                pageRaw.getTotalPages(),
                pageRaw.isLast()
        );
    }

    @Override
    public void deleteComment(String commentId) {
        BlogComment comment = blogCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException(AppConstants.Resource_Constants.COMMENT, AppConstants.Field_Constants.ID, commentId));

        BlogPost blogPost = blogPostRepository.findById(comment.getBlogId())
                .orElseThrow(() -> new ResourceNotFoundException(AppConstants.Resource_Constants.BLOG, AppConstants.Field_Constants.ID, comment.getBlogId()));

        String currentUserId = SecurityUtils.getCurrentUserIdOrThrow();

        // Comment owner OR Blog post owner OR Admin can delete
        boolean isCommentOwner = currentUserId.equals(comment.getCreatedBy());
        boolean isBlogPostOwner = currentUserId.equals(blogPost.getCreatedBy());
        boolean isAdmin = SecurityUtils.isAdmin();

        if (!isCommentOwner && !isBlogPostOwner && !isAdmin) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        blogCommentRepository.delete(comment);

        // Update commentsCount in BlogPost
        blogPost.setCommentsCount(blogCommentRepository.countByBlogId(blogPost.getId()));
        blogPostRepository.save(blogPost);

        log.info("Deleted comment: id={}", commentId);
    }

    // === HELPER METHODS ===
    private BlogPostResponse mapToResponse(BlogPost blogPost) {
        BlogPostResponse response = blogPostMapper.entityToResponse(blogPost);
        response.setLikesCount((long) blogPost.getLikes().size());
        response.setReportsCount((long) blogPost.getReports().size());

        // Count comments
        response.setCommentsCount(blogCommentRepository.countByBlogId(blogPost.getId()));

        Optional<String> currentUserIdOpt = SecurityUtils.getCurrentUserId();
        if (currentUserIdOpt.isPresent()) {
            response.setIsLiked(blogPost.getLikes().contains(currentUserIdOpt.get()));
        } else {
            response.setIsLiked(false);
        }
        return response;
    }

    private void enrichUserProfiles(List<BlogPostResponse> responses) {
        if (responses == null || responses.isEmpty()) return;
        List<String> distinctUserIds = responses.stream()
                .map(BlogPostResponse::getCreatedBy)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<String, UserSharedProfile> profileMap = fetchProfilesFromRedis(distinctUserIds);
        responses.forEach(r -> {
            if (r.getCreatedBy() != null) {
                r.setAuthor(profileMap.get(r.getCreatedBy()));
            }
        });
    }

    private void enrichUserProfilesForComments(List<BlogCommentResponse> responses) {
        if (responses == null || responses.isEmpty()) return;
        List<String> distinctUserIds = responses.stream()
                .map(BlogCommentResponse::getCreatedBy)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<String, UserSharedProfile> profileMap = fetchProfilesFromRedis(distinctUserIds);
        responses.forEach(r -> {
            if (r.getCreatedBy() != null) {
                r.setAuthor(profileMap.get(r.getCreatedBy()));
            }
        });
    }

    private Map<String, UserSharedProfile> fetchProfilesFromRedis(List<String> userIds) {
        Map<String, UserSharedProfile> profileMap = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) return profileMap;

        List<String> redisKeys = userIds.stream()
                .map(RedisPrefixConstants::getSharedUserProfileKey)
                .toList();

        try {
            List<Object> cachedProfiles = redisService.multiGet(redisKeys);
            for (int i = 0; i < userIds.size(); i++) {
                String uId = userIds.get(i);
                Object val = cachedProfiles != null && i < cachedProfiles.size() ? cachedProfiles.get(i) : null;
                if (val != null) {
                    if (val instanceof UserSharedProfile profile) {
                        profileMap.put(uId, profile);
                    } else {
                        UserSharedProfile profile = objectMapper.convertValue(val, UserSharedProfile.class);
                        profileMap.put(uId, profile);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to enrich user profiles from Redis: {}", e.getMessage(), e);
        }
        return profileMap;
    }

    private String toSlug(String input) {
        if (input == null || input.isBlank()) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String stripped = Pattern.compile("\\p{M}").matcher(normalized).replaceAll("");
        stripped = stripped.replace('đ', 'd').replace('Đ', 'D');
        String nowhitespace = Pattern.compile("\\s+").matcher(stripped).replaceAll("-");
        String slug = Pattern.compile("[^\\w-]").matcher(nowhitespace).replaceAll("");
        slug = Pattern.compile("-{2,}").matcher(slug).replaceAll("-");
        slug = slug.replaceAll("^-|-$", "");
        return slug.toLowerCase(Locale.ENGLISH);
    }

    @Override
    public BlogAdminStatsResponse getAdminBlogStats() {
        long total = mongoTemplate.count(new Query(), BlogPost.class);
        long published = mongoTemplate.count(new Query(Criteria.where("status").is(BlogStatus.PUBLISHED)), BlogPost.class);
        long blocked = mongoTemplate.count(new Query(Criteria.where("status").is(BlogStatus.BLOCKED)), BlogPost.class);
        long reported = mongoTemplate.count(new Query(Criteria.where("reports.0").exists(true)), BlogPost.class);

        return BlogAdminStatsResponse.builder()
                .totalBlogs(total)
                .publishedBlogs(published)
                .blockedBlogs(blocked)
                .reportedBlogs(reported)
                .build();
    }

    @Override
    public BlogInstructorStatsResponse getMyBlogStats() {
        String currentUserId = SecurityUtils.getCurrentUserIdOrThrow();

        long total = mongoTemplate.count(new Query(Criteria.where("createdBy").is(currentUserId)), BlogPost.class);
        long published = mongoTemplate.count(new Query(Criteria.where("createdBy").is(currentUserId).and("status").is(BlogStatus.PUBLISHED)), BlogPost.class);
        long draft = mongoTemplate.count(new Query(Criteria.where("createdBy").is(currentUserId).and("status").is(BlogStatus.DRAFT)), BlogPost.class);

        // Sum views and likes by fetching only viewsCount and likes fields
        Query sumQuery = new Query(Criteria.where("createdBy").is(currentUserId));
        sumQuery.fields().include("viewsCount").include("likes");
        List<BlogPost> posts = mongoTemplate.find(sumQuery, BlogPost.class);

        long accumulatedViews = 0;
        long accumulatedLikes = 0;
        for (BlogPost post : posts) {
            accumulatedViews += post.getViewsCount() != null ? post.getViewsCount() : 0;
            accumulatedLikes += post.getLikes() != null ? post.getLikes().size() : 0;
        }

        return BlogInstructorStatsResponse.builder()
                .totalBlogs(total)
                .publishedBlogs(published)
                .draftBlogs(draft)
                .accumulatedViews(accumulatedViews)
                .accumulatedLikes(accumulatedLikes)
                .build();
    }
}
