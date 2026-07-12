package com.hust.learningservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonProgressRequest {
    @NotBlank
    private String courseId;

    @NotBlank
    private String lessonId;

    @NotNull
    private Boolean isDone;

    private Double lastWatchedTime;
}
