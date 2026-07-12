package com.hust.courseservice.service.impl;

import com.hust.courseservice.repository.CourseRepository;
import com.hust.courseservice.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.hust.courseservice.dto.response.AuthorCourseReportResponse;
import com.hust.courseservice.dto.response.CourseInsightReportResponse;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final CourseRepository courseRepository;

    @Override
    public CourseInsightReportResponse getCourseInsights() {
        log.info("Generating course insights report");
        
        List<CourseInsightReportResponse> results = courseRepository.getCourseInsightsAggregation();
        
        if (results == null || results.isEmpty()) {
            return CourseInsightReportResponse.builder().build();
        }

        return results.get(0);
    }

    @Override
    public AuthorCourseReportResponse getCoursesReportByAuthor(String authorId) {
        log.info("Generating courses report for author {}", authorId);
        
        List<AuthorCourseReportResponse> results = courseRepository.getAuthorCourseReportAggregation(authorId);
        
        if (results == null || results.isEmpty()) {
            return AuthorCourseReportResponse.builder().instructorId(authorId).build();
        }

        return results.get(0);
    }
}
