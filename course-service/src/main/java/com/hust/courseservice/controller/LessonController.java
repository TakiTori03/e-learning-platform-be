package com.hust.courseservice.controller;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.commonlibrary.dto.ListResponse;
import com.hust.courseservice.dto.request.LessonRequest;
import com.hust.courseservice.dto.response.LessonResponse;
import com.hust.courseservice.service.LessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;

    @PostMapping
    public ResponseEntity<ApiResponse<LessonResponse>> create(@RequestBody LessonRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<LessonResponse>builder()
                        .success(true)
                        .payload(lessonService.create(request))
                        .build()
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<LessonResponse>> update(@PathVariable String id, @RequestBody LessonRequest request) {
        return ResponseEntity.ok(
                ApiResponse.<LessonResponse>builder()
                        .success(true)
                        .payload(lessonService.update(id, request))
                        .build()
        );
    }

    @PostMapping("/delete")
    public ResponseEntity<ApiResponse<Void>> delete(@RequestBody List<String> ids) {
        lessonService.delete(ids);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .build()
        );
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<ListResponse<LessonResponse>>> search(
            @RequestParam(name = "q", required = false) String text,
            @PageableDefault Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.<ListResponse<LessonResponse>>builder()
                        .success(true)
                        .payload(lessonService.search(text, pageable))
                        .build()
        );
    }


    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LessonResponse>> detail(@PathVariable String id) {
        return ResponseEntity.ok(
                ApiResponse.<LessonResponse>builder()
                        .success(true)
                        .payload(lessonService.detail(id))
                        .build()
        );
    }

    @GetMapping("/section/{sectionId}")
    public ResponseEntity<ApiResponse<List<LessonResponse>>> getBySectionId(@PathVariable String sectionId) {
        return ResponseEntity.ok(
                ApiResponse.<List<LessonResponse>>builder()
                        .success(true)
                        .payload(lessonService.getBySectionId(sectionId))
                        .build()
        );
    }

    @GetMapping("/section/course-enrolled/{sectionId}")
    public ResponseEntity<ApiResponse<List<LessonResponse>>> getBySectionIdEnrolled(@PathVariable String sectionId) {
        String userId = com.hust.commonlibrary.utils.SecurityUtils.getCurrentUserIdOrThrow();
        return ResponseEntity.ok(
                ApiResponse.<List<LessonResponse>>builder()
                        .success(true)
                        .payload(lessonService.getBySectionIdEnrolled(sectionId, userId))
                        .build()
        );
    }

    @GetMapping("/course/{courseId}/all-lessons")
    public ResponseEntity<ApiResponse<List<LessonResponse>>> getByCourseId(@PathVariable String courseId) {
        return ResponseEntity.ok(
                ApiResponse.<List<LessonResponse>>builder()
                        .success(true)
                        .payload(lessonService.getByCourseId(courseId))
                        .build()
        );
    }

    @PostMapping("/done/{id}")
    public ResponseEntity<ApiResponse<Void>> updateDone(@PathVariable String id) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        lessonService.updateDone(id, userId);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .build()
        );
    }

    @GetMapping("/lesson/{id}/users")
    public ResponseEntity<ApiResponse<List<String>>> getUsersByLessonId(@PathVariable String id) {
        return ResponseEntity.ok(
                ApiResponse.<List<String>>builder()
                        .success(true)
                        .payload(lessonService.getUsersByLessonId(id))
                        .build()
        );
    }

    @PostMapping("/reorder")
    public ResponseEntity<ApiResponse<Void>> reorder(@RequestBody List<String> lessonIds) {
        lessonService.reorder(lessonIds);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .build()
        );
    }

}
