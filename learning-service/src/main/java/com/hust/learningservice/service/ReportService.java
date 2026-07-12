package com.hust.learningservice.service;

import com.hust.learningservice.dto.response.CourseProgressReportResponse;

import java.util.List;

public interface ReportService {
    List<CourseProgressReportResponse> getUsersProgressReport();
}
