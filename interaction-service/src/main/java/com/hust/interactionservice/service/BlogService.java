package com.hust.interactionservice.service;

import com.hust.commonlibrary.dto.ListResponse;
import com.hust.interactionservice.dto.request.BlogPostRequest;
import com.hust.interactionservice.dto.request.BlogCommentRequest;
import com.hust.interactionservice.dto.request.BlogTopicRequest;
import com.hust.interactionservice.dto.response.BlogPostResponse;
import com.hust.interactionservice.dto.response.BlogCommentResponse;
import com.hust.interactionservice.dto.response.BlogTopicResponse;
import com.hust.interactionservice.dto.response.BlogAdminStatsResponse;
import com.hust.interactionservice.dto.response.BlogInstructorStatsResponse;
import com.hust.interactionservice.entity.enums.BlogStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BlogService {
    
    // === TOPIC MANAGEMENT ===
    List<BlogTopicResponse> getAllTopics();
    ListResponse<BlogTopicResponse> searchTopics(String text, Pageable pageable);
    BlogTopicResponse getTopicDetail(String id);
    BlogTopicResponse createTopic(BlogTopicRequest request);
    BlogTopicResponse updateTopic(String id, BlogTopicRequest request);
    void deleteTopic(String id);

    // === BLOG POST MANAGEMENT ===
    BlogPostResponse createBlogPost(BlogPostRequest request);
    BlogPostResponse updateBlogPost(String id, BlogPostRequest request);
    void deleteBlogPost(String id);
    BlogPostResponse publishBlogPost(String id);
    BlogPostResponse pinBlogPost(String id, Boolean pin);
    BlogPostResponse blockBlogPost(String id, Boolean block);
    
    BlogPostResponse getBlogPostDetail(String slug);
    ListResponse<BlogPostResponse> getPublishedBlogPosts(String topicId, String search, String tag, String authorId, List<String> excludeIds, Pageable pageable);
    List<BlogPostResponse> getFeaturedBlogPosts(int size);
    ListResponse<BlogPostResponse> getMyBlogPosts(String topicId, BlogStatus status, String search, Pageable pageable);
    ListResponse<BlogPostResponse> getAdminManagedBlogPosts(String topicId, BlogStatus status, String authorId, Boolean isPinned, Boolean hasReports, String search, Pageable pageable);
    BlogAdminStatsResponse getAdminBlogStats();
    BlogInstructorStatsResponse getMyBlogStats();

    // === INTERACTIONS (LIKE, REPORT & COMMENTS) ===
    void toggleLikeBlogPost(String id);
    void shareBlogPost(String id);
    void reportBlogPost(String id, String reason);

    BlogCommentResponse createComment(String blogId, BlogCommentRequest request);
    ListResponse<BlogCommentResponse> getComments(String blogId, Pageable pageable);
    void deleteComment(String commentId);
}
