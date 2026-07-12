package com.hust.interactionservice.dto.response;

import lombok.*;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingResult {
    private Double averageRating;
    private Long totalReviews;
    private Map<String, String> ratingPercentages;
}
