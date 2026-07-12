package com.hust.learningservice.service.impl;

import com.hust.learningservice.dto.response.CourseProgressReportResponse;
import com.hust.learningservice.repository.EnrollmentRepository;
import com.hust.learningservice.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final EnrollmentRepository enrollmentRepository;

    @Override
    public List<CourseProgressReportResponse> getUsersProgressReport() {
        log.info("Generating users progress report");
        
        List<CourseProgressReportResponse> results = enrollmentRepository.getUsersProgressAggregation();
        
        return results != null ? results : List.of();
    }
}
