package com.hust.learningservice.controller;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.commonlibrary.dto.ListResponse;
import com.hust.learningservice.dto.request.DiscussionRequest;
import com.hust.learningservice.dto.response.DiscussionResponse;
import com.hust.learningservice.service.DiscussionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/discussions")
@RequiredArgsConstructor
public class DiscussionController {

    private final DiscussionService discussionService;

    @PostMapping
    public ResponseEntity<ApiResponse<DiscussionResponse>> createDiscussion(@RequestBody @Valid DiscussionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<DiscussionResponse>builder()
                .success(true)
                .payload(discussionService.createDiscussion(request))
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DiscussionResponse>> getDiscussionById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.<DiscussionResponse>builder()
                .success(true)
                .payload(discussionService.getDiscussionById(id))
                .build());
    }

    @GetMapping("/lesson/{lessonId}")
    public ResponseEntity<ApiResponse<ListResponse<DiscussionResponse>>> getDiscussionTree(
            @PathVariable String lessonId,
            @PageableDefault Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.<ListResponse<DiscussionResponse>>builder()
                .success(true)
                .payload(discussionService.getDiscussionTreeByLesson(lessonId, pageable))
                .build());
    }

    @GetMapping("/section/{sectionId}")
    public ResponseEntity<ApiResponse<ListResponse<DiscussionResponse>>> getDiscussionTreeBySection(
            @PathVariable String sectionId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.<ListResponse<DiscussionResponse>>builder()
                .success(true)
                .payload(discussionService.getDiscussionTreeBySection(sectionId, pageable))
                .build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DiscussionResponse>> updateDiscussion(@PathVariable String id, 
                                                                           @RequestBody String content) {
        return ResponseEntity.ok(ApiResponse.<DiscussionResponse>builder()
                .success(true)
                .payload(discussionService.updateDiscussion(id, content))
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDiscussion(@PathVariable String id) {
        discussionService.deleteDiscussion(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Discussion deleted")
                .build());
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<ApiResponse<DiscussionResponse>> likeDiscussion(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.<DiscussionResponse>builder()
                .success(true)
                .payload(discussionService.likeDiscussion(id))
                .build());
    }

    @PostMapping("/{id}/dislike")
    public ResponseEntity<ApiResponse<DiscussionResponse>> dislikeDiscussion(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.<DiscussionResponse>builder()
                .success(true)
                .payload(discussionService.dislikeDiscussion(id))
                .build());
    }
}
