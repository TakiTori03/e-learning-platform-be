package com.hust.interactionservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalCourseRatingResponse {
    private String courseId;
    private Double avgRatingStars;
    private Long numOfReviews;
}
