package com.hust.interactionservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRequest {
    @NotBlank
    private String courseId;


    @NotBlank
    private String title;

    @NotBlank
    private String content;

    @NotNull
    @DecimalMin(value = "0.5")
    @DecimalMax(value = "5.0")
    private Double ratingStar;
}
