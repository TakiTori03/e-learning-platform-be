package com.hust.interactionservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistRequest {
    @NotBlank
    private String courseId;
}
