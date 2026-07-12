package com.hust.courseservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseInsightReportResponse {
    private Long totalCourses;
    private Long totalActiveCourses;
    private Long totalDraftCourses;
    private Double averageRating;
    private Long totalReviews;
    private Long totalViews;
}
