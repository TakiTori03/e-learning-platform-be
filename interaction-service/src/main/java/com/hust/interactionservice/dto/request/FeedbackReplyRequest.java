package com.hust.interactionservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FeedbackReplyRequest {
    @NotBlank
    private String content;
}
