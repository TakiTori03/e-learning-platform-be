package com.hust.commonlibrary.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Sự kiện kích hoạt khi bài học liên kết hoặc hủy liên kết với một Đề thi (Quiz).
 * Producer: course-service
 * Consumer: assessment-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonQuizLinkedEvent {
    private String courseId;
    private String lessonId;
    private String quizId;
    private com.hust.commonlibrary.enums.LinkAction action; // "LINK" hoặc "UNLINK"
}
