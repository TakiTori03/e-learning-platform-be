package com.hust.learningservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscussionRequest {
    private String courseId;
    private String sectionId;
    private String lessonId;

    @NotBlank
    private String content;

    private String parentId;
}
