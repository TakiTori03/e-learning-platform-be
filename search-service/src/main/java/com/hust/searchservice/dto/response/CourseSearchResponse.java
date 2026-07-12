package com.hust.searchservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseSearchResponse {
    private String id;
    private String name;
    private String subTitle;
    private String description;
    private Double price;
    private Double finalPrice;
    private Double avgRatingStars;
    private Integer studentCount;
    private Integer numOfReviews;
    private String level;
    private String status;
    private String instructorId;
    
    private CategoryDto category;
    private Instant createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryDto {
        private String id;
        private String name;
    }
}
