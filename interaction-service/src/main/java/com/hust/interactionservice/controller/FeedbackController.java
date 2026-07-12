package com.hust.interactionservice.controller;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.commonlibrary.dto.ListResponse;
import com.hust.interactionservice.dto.request.FeedbackReplyRequest;
import com.hust.interactionservice.dto.request.FeedbackRequest;
import com.hust.interactionservice.dto.response.FeedbackReplyResponse;
import com.hust.interactionservice.dto.response.FeedbackResponse;
import com.hust.interactionservice.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/feedbacks")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    public ResponseEntity<ApiResponse<FeedbackResponse>> create(@RequestBody @Valid FeedbackRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<FeedbackResponse>builder()
                        .success(true)
                        .payload(feedbackService.createFeedback(request))
                        .build()
        );
    }


    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FeedbackResponse>> getDetail(@PathVariable String id) {
        return ResponseEntity.ok(
                ApiResponse.<FeedbackResponse>builder()
                        .success(true)
                        .payload(feedbackService.getFeedbackDetail(id))
                        .build()
        );
    }

 
    @GetMapping("/admin/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ListResponse<FeedbackResponse>>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) com.hust.interactionservice.constant.FeedbackType type,
            @RequestParam(required = false) com.hust.interactionservice.constant.FeedbackStatus status,
            @PageableDefault Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.<ListResponse<FeedbackResponse>>builder()
                        .success(true)
                        .payload(feedbackService.getAllFeedbacks(q, type, status, pageable))
                        .build()
        );
    }

    @PostMapping("/admin/feedbacks/{id}/reply")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FeedbackReplyResponse>> replyFeedback(
            @PathVariable String id,
            @RequestBody @Valid FeedbackReplyRequest request) {
        return ResponseEntity.ok(
                ApiResponse.<FeedbackReplyResponse>builder()
                        .success(true)
                        .payload(feedbackService.replyFeedback(id, request))
                        .build()
        );
    }

    @PutMapping("/admin/feedbacks/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FeedbackResponse>> updateFeedbackStatus(
            @PathVariable String id,
            @RequestParam com.hust.interactionservice.constant.FeedbackStatus status) {
        return ResponseEntity.ok(
                ApiResponse.<FeedbackResponse>builder()
                        .success(true)
                        .payload(feedbackService.updateFeedbackStatus(id, status))
                        .build()
        );
    }
}
