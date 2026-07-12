package com.hust.learningservice.controller;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.commonlibrary.utils.SecurityUtils;
import com.hust.learningservice.dto.request.CourseNoteRequest;
import com.hust.learningservice.dto.response.CourseNoteResponse;
import com.hust.learningservice.service.LearningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notes")
@RequiredArgsConstructor
public class NoteController {

    private final LearningService learningService;

    @GetMapping("/{courseId}")
    public ApiResponse<List<CourseNoteResponse>> getMyNotes(@PathVariable String courseId) {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        return ApiResponse.<List<CourseNoteResponse>>builder()
                .success(true)
                .payload(learningService.getMyNotes(userId, courseId))
                .build();
    }

    @PostMapping
    public ApiResponse<Void> addNote(@RequestBody @Valid CourseNoteRequest request) {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        learningService.addNote(userId, request);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }

    @DeleteMapping("/{noteId}")
    public ApiResponse<Void> deleteNote(@PathVariable String noteId) {
        learningService.deleteNote(noteId);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }
}
