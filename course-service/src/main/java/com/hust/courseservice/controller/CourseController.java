package com.hust.courseservice.controller;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hust.commonlibrary.annotation.TrackView;
import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.commonlibrary.dto.ListResponse;
import com.hust.courseservice.dto.request.AdminApproveRequest;
import com.hust.courseservice.dto.request.CourseRequest;
import com.hust.courseservice.dto.response.ActionLogResponse;
import com.hust.courseservice.dto.response.AdminCourseStatsResponse;
import com.hust.courseservice.dto.response.CourseResponse;
import com.hust.courseservice.entity.enums.CourseStatus;
import com.hust.courseservice.service.CourseService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
@Slf4j
public class CourseController {

    private final CourseService courseService;

    @GetMapping("/count-by-categories")
    public ResponseEntity<ApiResponse<Map<String, Long>>> countByCategories(
            @RequestParam List<String> categoryIds) {
        return ResponseEntity.ok(
                ApiResponse.<Map<String, Long>>builder()
                        .success(true)
                        .payload(courseService.countByCategories(categoryIds))
                        .build()
        );
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<ListResponse<CourseResponse>>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) List<String> authors,
            @RequestParam(required = false) List<String> topics,
            @RequestParam(required = false) List<String> levels,
            @RequestParam(required = false) List<String> prices,
            @RequestParam(required = false) Double rating,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(
                ApiResponse.<ListResponse<CourseResponse>>builder()
                        .success(true)
                        .payload(courseService.search(q, authors, topics, levels, prices, rating, CourseStatus.PUBLISHED, pageable))
                        .build()
        );
    }

    @GetMapping("/search-elasticsearch")
    public ResponseEntity<ApiResponse<ListResponse<CourseResponse>>> searchElasticsearch(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) List<String> authors,
            @RequestParam(required = false) List<String> topics,
            @RequestParam(required = false) List<String> levels,
            @RequestParam(required = false) List<String> prices,
            @RequestParam(required = false) Double rating,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(
                ApiResponse.<ListResponse<CourseResponse>>builder()
                        .success(true)
                        .payload(courseService.searchElasticsearch(q, authors, topics, levels, prices, rating, pageable))
                        .build()
        );
    }

    @GetMapping("/suggest")
    public ResponseEntity<ApiResponse<List<String>>> suggest(
            @RequestParam String q,
            @RequestParam(defaultValue = "5") int limit) {

        return ResponseEntity.ok(
                ApiResponse.<List<String>>builder()
                        .success(true)
                        .payload(courseService.suggestCourses(q, limit))
                        .build()
        );
    }



    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> detail(@PathVariable String id) {
        return ResponseEntity.ok(
                ApiResponse.<CourseResponse>builder()
                        .success(true)
                        .payload(courseService.detail(id))
                        .build()
        );
    }

    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getPopularCourses(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(
                ApiResponse.<List<CourseResponse>>builder()
                        .success(true)
                        .payload(courseService.getPopularCourses(limit))
                        .build()
        );
    }

    @GetMapping("/trending")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getTrendingCourses(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(
                ApiResponse.<List<CourseResponse>>builder()
                        .success(true)
                        .payload(courseService.getTrendingCourses(limit))
                        .build()
        );
    }

    @GetMapping("/recommended")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getRecommendedCourses(
            @RequestParam(required = false) List<String> enrolledIds,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(
                ApiResponse.<List<CourseResponse>>builder()
                        .success(true)
                        .payload(courseService.getRecommendedCourses(enrolledIds, limit))
                        .build()
        );
    }

    @GetMapping("/related/{courseId}")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getRelatedCourses(
            @PathVariable String courseId,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(
                ApiResponse.<List<CourseResponse>>builder()
                        .success(true)
                        .payload(courseService.getRelatedCourses(courseId, limit))
                        .build()
        );
    }

//    @GetMapping("/detail/{id}")
//    public ResponseEntity<ApiResponse<CourseResponse>> getFullDetail(@PathVariable String id) {
//        return ResponseEntity.ok(
//                ApiResponse.<CourseResponse>builder()
//                        .success(true)
//                        .payload(courseService.getFullDetail(id))
//                        .build()
//        );
//    }

    @PostMapping("/increase-view/{id}")
    @TrackView(type = "course", value = "#id")
    public ResponseEntity<ApiResponse<Void>> increaseView(@PathVariable String id) {
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .build()
        );
    }

    @PostMapping("/{id}/view")
    @TrackView(type = "course", value = "#id")
    public ResponseEntity<ApiResponse<Void>> recordView(@PathVariable String id) {
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .build()
        );
    }



    @GetMapping("/all-active")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getAllActiveCourses() {
        return ResponseEntity.ok(
                ApiResponse.<List<CourseResponse>>builder()
                        .success(true)
                        .payload(courseService.getAllActiveCourses())
                        .build()
        );
    }

    @PostMapping("/ids")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getCoursesByIds(@RequestBody List<String> ids) {
        return ResponseEntity.ok(
                ApiResponse.<List<CourseResponse>>builder()
                        .success(true)
                        .payload(courseService.getBulkCourses(ids))
                        .build()
        );
    }

    @GetMapping("/select")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getSelect() {
        return ResponseEntity.ok(
                ApiResponse.<List<CourseResponse>>builder()
                        .success(true)
                        .payload(courseService.getAllActiveCourses())
                        .build()
        );
    }

    @GetMapping("/admin/courses/{id}/histories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ListResponse<ActionLogResponse>>> getHistories(
            @PathVariable String id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(
                ApiResponse.<ListResponse<ActionLogResponse>>builder()
                        .success(true)
                        .payload(courseService.getHistories(id, page, limit))
                        .build()
        );
    }

    // --- Instructor Methods ---
    @PostMapping
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<CourseResponse>> create(@RequestBody CourseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<CourseResponse>builder()
                        .success(true)
                        .payload(courseService.create(request))
                        .build()
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<CourseResponse>> update(@PathVariable String id, @RequestBody CourseRequest request) {
        return ResponseEntity.ok(
                ApiResponse.<CourseResponse>builder()
                        .success(true)
                        .payload(courseService.update(id, request))
                        .build()
        );
    }

    @PutMapping("/{courseId}/final-exam")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<CourseResponse>> linkFinalExam(
            @PathVariable String courseId,
            @RequestParam(required = false) String finalExamId) {
        log.info("Instructor link final exam: courseId={}, finalExamId={}", courseId, finalExamId);
        String instructorId = com.hust.commonlibrary.utils.SecurityUtils.getCurrentUserIdOrThrow();
        CourseResponse response = courseService.linkFinalExam(courseId, finalExamId, instructorId);
        return ResponseEntity.ok(
                ApiResponse.<CourseResponse>builder()
                        .success(true)
                        .payload(response)
                        .build()
        );
    }

    @PostMapping("/delete")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<Void>> delete(@RequestBody List<String> ids) {
        courseService.delete(ids);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .build()
        );
    }

    @PatchMapping("/update-active-status/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<Void>> updateStatus(
            @PathVariable String id,
            @RequestParam(required = false) CourseStatus status) {
        courseService.updateStatus(id, status);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .build()
        );
    }

    @GetMapping("/instructor/search")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<ListResponse<CourseResponse>>> instructorSearch(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) List<String> topics,
            @RequestParam(required = false) List<String> levels,
            @RequestParam(required = false) List<String> prices,
            @RequestParam(required = false) Double rating,
            @RequestParam(required = false) CourseStatus status,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        String instructorId = com.hust.commonlibrary.utils.SecurityUtils.getCurrentUserIdOrThrow();
        log.info("Instructor {} is searching their courses with query q: {}, status: {}", instructorId, q, status);
        return ResponseEntity.ok(
                ApiResponse.<ListResponse<CourseResponse>>builder()
                        .success(true)
                        .payload(courseService.search(q, List.of(instructorId), topics, levels, prices, rating, status, pageable))
                        .build()
        );
    }

    @GetMapping("/instructor/all")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getAllCourseForInstructor() {
        return ResponseEntity.ok(
                ApiResponse.<List<CourseResponse>>builder()
                        .success(true)
                        .payload(courseService.getAllCoursesForInstructor())
                        .build()
        );
    }

    // --- Admin Methods ---
    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> approve(
            @PathVariable String id,
            @RequestBody AdminApproveRequest request) {
        courseService.adminUpdateStatus(id, request.getStatus(), request.getNote());
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Course status updated successfully")
                        .build()
        );
    }

    @GetMapping("/admin/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ListResponse<CourseResponse>>> adminSearch(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) List<String> authors,
            @RequestParam(required = false) List<String> topics,
            @RequestParam(required = false) List<String> levels,
            @RequestParam(required = false) List<String> prices,
            @RequestParam(required = false) Double rating,
            @RequestParam(required = false) CourseStatus status,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.<ListResponse<CourseResponse>>builder()
                        .success(true)
                        .payload(courseService.search(q, authors, topics, levels, prices, rating, status, pageable))
                        .build()
        );
    }

    @GetMapping("/admin/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<AdminCourseStatsResponse>> getAdminCourseStats() {

        return ResponseEntity.ok(
                ApiResponse.<AdminCourseStatsResponse>builder()
                        .success(true)
                        .payload(courseService.getAdminCourseStats())
                        .build()
        );
    }

    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getBulkCourses(@RequestBody List<String> courseIds) {
        return ResponseEntity.ok(
                ApiResponse.<List<CourseResponse>>builder()
                        .success(true)
                        .payload(courseService.getBulkCourses(courseIds))
                        .build()
        );
    }
}
