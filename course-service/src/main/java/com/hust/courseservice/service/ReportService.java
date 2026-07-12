package com.hust.courseservice.service;

import com.hust.courseservice.dto.response.AuthorCourseReportResponse;
import com.hust.courseservice.dto.response.CourseInsightReportResponse;

import java.util.List;

public interface ReportService {
    CourseInsightReportResponse getCourseInsights();

    AuthorCourseReportResponse getCoursesReportByAuthor(String authorId);
}
