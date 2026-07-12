package com.hust.courseservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCourseStatsResponse {
    private long totalCourses;
    private long publishedCourses;
    private long pendingCourses;
    private long draftCourses;
    private long rejectedCourses;
    private long archivedCourses;
}
