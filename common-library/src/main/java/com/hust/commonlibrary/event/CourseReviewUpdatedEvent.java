package com.hust.commonlibrary.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Sự kiện phát ra từ interaction-service mỗi khi có thay đổi đánh giá (tạo/sửa/xóa review).
 * course-service lắng nghe sự kiện này để cập nhật avgRatingStars và numOfReviews.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseReviewUpdatedEvent {
    private String courseId;
    private Double avgRatingStars;
    private Long numOfReviews;
}
