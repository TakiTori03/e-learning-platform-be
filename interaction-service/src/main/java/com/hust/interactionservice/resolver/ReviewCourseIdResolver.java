package com.hust.interactionservice.resolver;

import com.hust.commonlibrary.resolver.CourseIdResolver;
import com.hust.interactionservice.entity.Review;
import com.hust.interactionservice.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Tự động tra cứu courseId THẬT từ Review ID trong MongoDB.
 * Cho phép Aspect kiểm duyệt xem Giảng viên hiện tại có quyền Phản hồi Review này hay không.
 */
@Component("reviewResolver")
@RequiredArgsConstructor
public class ReviewCourseIdResolver implements CourseIdResolver {

    private final ReviewRepository reviewRepository;

    @Override
    public String resolveCourseId(String domainId) {
        if (domainId == null || domainId.isBlank()) return null;

        return reviewRepository.findById(domainId)
                .map(Review::getCourseId)
                .orElse(null);
    }
}
