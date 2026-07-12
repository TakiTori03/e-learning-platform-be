package com.hust.interactionservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogPostRequest {
    @NotBlank
    private String title;

    @NotBlank
    private String content;

    private String thumbnail;

    @NotBlank
    private String topicId;

    private List<String> tags;

    private List<String> courseIds;
}
