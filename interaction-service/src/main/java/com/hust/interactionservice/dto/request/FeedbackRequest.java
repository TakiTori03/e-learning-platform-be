package com.hust.interactionservice.dto.request;

import com.hust.interactionservice.constant.FeedbackType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FeedbackRequest {
    
    @NotNull
    private FeedbackType type;

    @NotBlank
    private String name;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String title;
    
    @NotBlank
    private String content;
}
