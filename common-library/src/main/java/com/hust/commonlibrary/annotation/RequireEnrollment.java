package com.hust.commonlibrary.annotation;

import java.lang.annotation.*;

/**
 * Custom Annotation để bảo vệ các tài nguyên học tập của Học viên (Students).
 * Chặn đứng 100% các học viên chưa đăng ký/mua khóa học mà cố tình truy cập.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireEnrollment {
    
    /**
     * Biểu thức SpEL để lấy courseId từ tham số đầu vào.
     * Ví dụ: "#courseId" hoặc "#request.courseId"
     */
    String courseId() default "";
    
    /**
     * Tên của Spring Bean chịu trách nhiệm check quyền (phải implement EnrollmentChecker).
     * Ví dụ: "learningEnrollmentChecker" hoặc "feignEnrollmentChecker"
     */
    String checker() default "";
}
