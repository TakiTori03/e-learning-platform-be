package com.hust.courseservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorCourseReportResponse {
    private String instructorId;
    private Long totalCourses;
    private Long totalViews;
    private Double averageRating;
    private Long totalReviews;
    private Long studentCount;
}
