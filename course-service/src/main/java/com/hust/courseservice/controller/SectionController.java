package com.hust.courseservice.controller;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.commonlibrary.dto.ListResponse;
import com.hust.courseservice.dto.request.SectionRequest;
import com.hust.courseservice.dto.response.SectionResponse;
import com.hust.courseservice.service.SectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sections")
@RequiredArgsConstructor
public class SectionController {

    private final SectionService sectionService;

    @PostMapping
    public ResponseEntity<ApiResponse<SectionResponse>> create(@RequestBody SectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<SectionResponse>builder()
                        .success(true)
                        .payload(sectionService.create(request))
                        .build()
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SectionResponse>> update(@PathVariable String id, @RequestBody SectionRequest request) {
        return ResponseEntity.ok(
                ApiResponse.<SectionResponse>builder()
                        .success(true)
                        .payload(sectionService.update(id, request))
                        .build()
        );
    }

    @PostMapping("/delete")
    public ResponseEntity<ApiResponse<Void>> delete(@RequestBody List<String> ids) {
        sectionService.delete(ids);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .build()
        );
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<ListResponse<SectionResponse>>> search(
            @RequestParam(name = "q", required = false) String text,
            @PageableDefault Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.<ListResponse<SectionResponse>>builder()
                        .success(true)
                        .payload(sectionService.search(text, pageable))
                        .build()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SectionResponse>> detail(@PathVariable String id) {
        return ResponseEntity.ok(
                ApiResponse.<SectionResponse>builder()
                        .success(true)
                        .payload(sectionService.detail(id))
                        .build()
        );
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<ApiResponse<List<SectionResponse>>> getByCourseId(@PathVariable String courseId) {
        return ResponseEntity.ok(
                ApiResponse.<List<SectionResponse>>builder()
                        .success(true)
                        .payload(sectionService.getByCourseId(courseId))
                        .build()
        );
    }

    @PostMapping("/reorder")
    public ResponseEntity<ApiResponse<Void>> reorder(@RequestBody List<String> sectionIds) {
        sectionService.reorder(sectionIds);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .build()
        );
    }

}
