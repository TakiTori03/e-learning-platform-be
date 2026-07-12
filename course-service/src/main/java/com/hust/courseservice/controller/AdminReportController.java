package com.hust.courseservice.controller;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.courseservice.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hust.courseservice.dto.response.AuthorCourseReportResponse;
import com.hust.courseservice.dto.response.CourseInsightReportResponse;

import java.util.List;

@RestController
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final ReportService reportService;

    /**
     * Tương đương API: /admin/report/course-insights
     */
    @GetMapping("/course-insights")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CourseInsightReportResponse>> getCourseInsights() {
        return ResponseEntity.ok(
                ApiResponse.<CourseInsightReportResponse>builder()
                        .success(true)
                        .payload(reportService.getCourseInsights())
                        .build()
        );
    }

    /**
     * Tương đương API: /admin/report/courses-report-by-author
     */
    @GetMapping("/courses-by-author")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<AuthorCourseReportResponse>> getCoursesReportByAuthor(
            @RequestParam(required = false) String authorId
    ) {
        if (!com.hust.commonlibrary.utils.SecurityUtils.isAdmin()) {
            authorId = com.hust.commonlibrary.utils.SecurityUtils.getCurrentUserIdOrThrow();
        }

        return ResponseEntity.ok(
                ApiResponse.<AuthorCourseReportResponse>builder()
                        .success(true)
                        .payload(reportService.getCoursesReportByAuthor(authorId))
                        .build()
        );
    }
}
