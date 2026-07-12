package com.hust.learningservice.controller;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.learningservice.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hust.learningservice.dto.response.CourseProgressReportResponse;

import java.util.List;

@RestController
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {

    private final ReportService reportService;

    /**
     * Tương đương API: /admin/report/users-progress
     */
    @GetMapping("/users-progress")
    public ResponseEntity<ApiResponse<List<CourseProgressReportResponse>>> getUsersProgress() {
        return ResponseEntity.ok(
                ApiResponse.<List<CourseProgressReportResponse>>builder()
                        .success(true)
                        .payload(reportService.getUsersProgressReport())
                        .build()
        );
    }
}
