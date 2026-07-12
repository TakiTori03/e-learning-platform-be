package com.hust.commonlibrary.resolver;

/**
 * Interface cầu nối cho phép AOP kiểm tra xem Học viên (userId)
 * có thực sự đã đăng ký/mua Khóa học (courseId) hay chưa.
 */
public interface EnrollmentChecker {
    /**
     * Xác thực quyền truy cập của học viên vào khóa học.
     * @param userId ID của học viên hiện tại.
     * @param courseId ID của khóa học muốn truy cập.
     * @return true nếu được phép, false nếu bị từ chối.
     */
    boolean hasAccess(String userId, String courseId);
}
