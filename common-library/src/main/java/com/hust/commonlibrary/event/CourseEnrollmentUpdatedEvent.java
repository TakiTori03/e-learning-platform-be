package com.hust.commonlibrary.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Sự kiện phát ra từ learning-service mỗi khi có ghi danh thành công.
 * course-service lắng nghe sự kiện này để cập nhật studentCount.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseEnrollmentUpdatedEvent {
    private String courseId;
    private Long studentCount;
}
