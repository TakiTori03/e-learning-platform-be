package com.hust.learningservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseProgressReportResponse {
    private String courseId;
    private Double averageProgress;
    private Long totalEnrollments;
    private Long completedEnrollments;
}
