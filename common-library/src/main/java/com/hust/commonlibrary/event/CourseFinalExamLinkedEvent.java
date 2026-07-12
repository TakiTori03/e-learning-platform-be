package com.hust.commonlibrary.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Triggered when a Course is linked or unlinked to a Final Exam Quiz.
 * Producer: course-service
 * Consumer: assessment-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseFinalExamLinkedEvent {
    private String courseId;
    private String finalExamId;
    private com.hust.commonlibrary.enums.LinkAction action; // "LINK" hoặc "UNLINK"
}
