package com.hust.courseservice.controller;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.commonlibrary.dto.ListResponse;
import com.hust.courseservice.dto.request.CategoryRequest;
import com.hust.courseservice.dto.response.CategoryResponse;
import com.hust.courseservice.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> create(@RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<CategoryResponse>builder()
                        .success(true)
                        .payload(categoryService.create(request))
                        .build()
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(@PathVariable String id, @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(
                ApiResponse.<CategoryResponse>builder()
                        .success(true)
                        .payload(categoryService.update(id, request))
                        .build()
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        categoryService.delete(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .build()
        );
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<ListResponse<CategoryResponse>>> search(
            @RequestParam(name = "q", required = false) String text,
            @PageableDefault Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.<ListResponse<CategoryResponse>>builder()
                        .success(true)
                        .payload(categoryService.search(text, pageable))
                        .build()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> detail(@PathVariable String id) {
        return ResponseEntity.ok(
                ApiResponse.<CategoryResponse>builder()
                        .success(true)
                        .payload(categoryService.detail(id))
                        .build()
        );
    }

    @GetMapping("/select")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getSelect(
            @RequestParam(name = "q", required = false) String q) {
        return ResponseEntity.ok(
                ApiResponse.<List<CategoryResponse>>builder()
                        .success(true)
                        .payload(categoryService.getSelect(q))
                        .build()
        );
    }



    @PatchMapping("/{id}/active-status")
    public ResponseEntity<ApiResponse<Void>> updateStatus(@PathVariable String id) {
        categoryService.updateStatus(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .build()
        );
    }
}
