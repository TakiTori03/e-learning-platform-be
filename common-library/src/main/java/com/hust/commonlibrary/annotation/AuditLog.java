package com.hust.commonlibrary.annotation;

import java.lang.annotation.*;

/**
 * Custom Annotation đánh dấu các API nhạy cảm cần được ghi Nhật ký Kiểm toán (Audit Log).
 * Ví dụ: Xóa tài khoản, Cập nhật điểm, Rút tiền, Xóa khóa học.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {

    /**
     * Tên hành động kiểm toán.
     * Ví dụ: "DELETE_COURSE", "UPDATE_PROFILE", "BAN_USER"
     */
    String action();

    /**
     * Cú pháp SpEL để trích xuất ID của đối tượng bị tác động.
     * Ví dụ: "#id" hoặc "#request.courseId"
     */
    String targetId() default "";

    /**
     * Mô tả ngắn gọn về hành động (tùy chọn).
     */
    String description() default "";
}
