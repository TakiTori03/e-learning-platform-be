package com.hust.identityservice.controller;


import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.identityservice.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hust.identityservice.dto.response.SignupReportResponse;
import com.hust.identityservice.dto.response.UserResponse;

import java.util.List;

@RestController
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final ReportService reportService;

    /**
     * Tương đương API: /admin/report/new-signups
     */
    @GetMapping("/new-signups")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SignupReportResponse>> getNewSignups(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "day") String groupBy
    ) {
        return ResponseEntity.ok(
                ApiResponse.<SignupReportResponse>builder()
                        .success(true)
                        .payload(reportService.getNewSignupsReport(startDate, endDate, groupBy))
                        .build()
        );
    }

    /**
     * Tương đương API: /admin/report/get-top-users
     */
    @GetMapping("/top-users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getTopUsers(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(
                ApiResponse.<List<UserResponse>>builder()
                        .success(true)
                        .payload(reportService.getTopUsers(limit))
                        .build()
        );
    }
}
