package com.hust.interactionservice.controller;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.commonlibrary.dto.ListResponse;
import com.hust.interactionservice.dto.request.BlogCommentRequest;
import com.hust.interactionservice.dto.request.BlogReportRequest;
import com.hust.interactionservice.dto.request.BlogPostRequest;
import com.hust.interactionservice.dto.request.BlogTopicRequest;
import com.hust.interactionservice.dto.response.BlogCommentResponse;
import com.hust.interactionservice.dto.response.BlogPostResponse;
import com.hust.interactionservice.dto.response.BlogTopicResponse;
import com.hust.interactionservice.dto.response.BlogAdminStatsResponse;
import com.hust.interactionservice.dto.response.BlogInstructorStatsResponse;
import com.hust.interactionservice.entity.enums.BlogStatus;
import com.hust.interactionservice.service.BlogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/blogs")
@RequiredArgsConstructor
public class BlogController {

    private final BlogService blogService;

    // === TOPIC ENDPOINTS ===

    @PostMapping("/topics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BlogTopicResponse>> createTopic(@RequestBody @Valid BlogTopicRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<BlogTopicResponse>builder()
                        .success(true)
                        .payload(blogService.createTopic(request))
                        .build()
        );
    }

    @PutMapping("/topics/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BlogTopicResponse>> updateTopic(@PathVariable String id, @RequestBody @Valid BlogTopicRequest request) {
        return ResponseEntity.ok(
                ApiResponse.<BlogTopicResponse>builder()
                        .success(true)
                        .payload(blogService.updateTopic(id, request))
                        .build()
        );
    }

    @DeleteMapping("/topics/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTopic(@PathVariable String id) {
        blogService.deleteTopic(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .build()
        );
    }

    @GetMapping("/topics/search")
    public ResponseEntity<ApiResponse<ListResponse<BlogTopicResponse>>> searchTopics(
            @RequestParam(name = "q", required = false) String text,
            @PageableDefault Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.<ListResponse<BlogTopicResponse>>builder()
                        .success(true)
                        .payload(blogService.searchTopics(text, pageable))
                        .build()
        );
    }

    @GetMapping("/topics/{id}")
    public ResponseEntity<ApiResponse<BlogTopicResponse>> getTopicDetail(@PathVariable String id) {
        return ResponseEntity.ok(
                ApiResponse.<BlogTopicResponse>builder()
                        .success(true)
                        .payload(blogService.getTopicDetail(id))
                        .build()
        );
    }

    @GetMapping("/topics/select")
    public ResponseEntity<ApiResponse<List<BlogTopicResponse>>> getSelect() {
        return ResponseEntity.ok(
                ApiResponse.<List<BlogTopicResponse>>builder()
                        .success(true)
                        .payload(blogService.getAllTopics())
                        .build()
        );
    }

    // === BLOG POST ENDPOINTS ===

    @PostMapping
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<BlogPostResponse>> createBlogPost(@RequestBody @Valid BlogPostRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<BlogPostResponse>builder()
                .success(true)
                .payload(blogService.createBlogPost(request))
                .build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<BlogPostResponse>> updateBlogPost(@PathVariable String id, @RequestBody @Valid BlogPostRequest request) {
        return ResponseEntity.ok(ApiResponse.<BlogPostResponse>builder()
                .success(true)
                .payload(blogService.updateBlogPost(id, request))
                .build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<Void>> deleteBlogPost(@PathVariable String id) {
        blogService.deleteBlogPost(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Deleted blog post")
                .build());
    }

    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<BlogPostResponse>> publishBlogPost(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.<BlogPostResponse>builder()
                .success(true)
                .payload(blogService.publishBlogPost(id))
                .build());
    }

    @PatchMapping("/{id}/pin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BlogPostResponse>> pinBlogPost(@PathVariable String id, @RequestParam Boolean pin) {
        return ResponseEntity.ok(ApiResponse.<BlogPostResponse>builder()
                .success(true)
                .payload(blogService.pinBlogPost(id, pin))
                .build());
    }

    @PatchMapping("/{id}/block")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BlogPostResponse>> blockBlogPost(@PathVariable String id, @RequestParam Boolean block) {
        return ResponseEntity.ok(ApiResponse.<BlogPostResponse>builder()
                .success(true)
                .payload(blogService.blockBlogPost(id, block))
                .build());
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<BlogPostResponse>> getBlogPostDetail(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.<BlogPostResponse>builder()
                .success(true)
                .payload(blogService.getBlogPostDetail(slug))
                .build());
    }

    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<BlogPostResponse>>> getFeaturedBlogPosts(
            @RequestParam(value = "size", defaultValue = "5") int size) {
        return ResponseEntity.ok(ApiResponse.<List<BlogPostResponse>>builder()
                .success(true)
                .payload(blogService.getFeaturedBlogPosts(size))
                .build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ListResponse<BlogPostResponse>>> getPublishedBlogPosts(
            @RequestParam(value = "topic", required = false) String topicId,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "tag", required = false) String tag,
            @RequestParam(value = "author", required = false) String authorId,
            @RequestParam(value = "excludeIds", required = false) List<String> excludeIds,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.<ListResponse<BlogPostResponse>>builder()
                .success(true)
                .payload(blogService.getPublishedBlogPosts(topicId, search, tag, authorId, excludeIds, pageable))
                .build());
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<ListResponse<BlogPostResponse>>> getMyBlogPosts(
            @RequestParam(value = "topic", required = false) String topicId,
            @RequestParam(value = "status", required = false) BlogStatus status,
            @RequestParam(value = "search", required = false) String search,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.<ListResponse<BlogPostResponse>>builder()
                .success(true)
                .payload(blogService.getMyBlogPosts(topicId, status, search, pageable))
                .build());
    }

    @GetMapping("/me/stats")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<BlogInstructorStatsResponse>> getMyBlogStats() {
        return ResponseEntity.ok(ApiResponse.<BlogInstructorStatsResponse>builder()
                .success(true)
                .payload(blogService.getMyBlogStats())
                .build());
    }

    @GetMapping("/admin/manage")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ListResponse<BlogPostResponse>>> getAdminManagedBlogPosts(
            @RequestParam(value = "topic", required = false) String topicId,
            @RequestParam(value = "status", required = false) BlogStatus status,
            @RequestParam(value = "author", required = false) String authorId,
            @RequestParam(value = "isPinned", required = false) Boolean isPinned,
            @RequestParam(value = "hasReports", required = false) Boolean hasReports,
            @RequestParam(value = "search", required = false) String search,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.<ListResponse<BlogPostResponse>>builder()
                .success(true)
                .payload(blogService.getAdminManagedBlogPosts(topicId, status, authorId, isPinned, hasReports, search, pageable))
                .build());
    }

    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BlogAdminStatsResponse>> getAdminBlogStats() {
        return ResponseEntity.ok(ApiResponse.<BlogAdminStatsResponse>builder()
                .success(true)
                .payload(blogService.getAdminBlogStats())
                .build());
    }

    // === INTERACTION ENDPOINTS ===

    @PostMapping("/{id}/like")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> toggleLikeBlogPost(@PathVariable String id) {
        blogService.toggleLikeBlogPost(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .build());
    }

    @PostMapping("/{id}/share")
    public ResponseEntity<ApiResponse<Void>> shareBlogPost(@PathVariable String id) {
        blogService.shareBlogPost(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .build());
    }

    @PostMapping("/{id}/report")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> reportBlogPost(@PathVariable String id, @RequestBody @Valid BlogReportRequest request) {
        blogService.reportBlogPost(id, request.getReason());
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Report submitted successfully")
                .build());
    }

    @PostMapping("/{id}/comments")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<BlogCommentResponse>> createComment(@PathVariable String id, @RequestBody @Valid BlogCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<BlogCommentResponse>builder()
                .success(true)
                .payload(blogService.createComment(id, request))
                .build());
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<ListResponse<BlogCommentResponse>>> getComments(
            @PathVariable String id,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.<ListResponse<BlogCommentResponse>>builder()
                .success(true)
                .payload(blogService.getComments(id, pageable))
                .build());
    }

    @DeleteMapping("/comments/{commentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteComment(@PathVariable String commentId) {
        blogService.deleteComment(commentId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Comment deleted")
                .build());
    }
}
