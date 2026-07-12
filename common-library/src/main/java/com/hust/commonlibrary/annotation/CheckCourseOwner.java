package com.hust.commonlibrary.annotation;

import java.lang.annotation.*;

/**
 * Custom Annotation Tối thượng: Hỗ trợ phân quyền cả Tạo mới, Cập nhật và Xóa
 * mà không bị lỗi Hack giả mạo ID!
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CheckCourseOwner {
    
    /**
     * Biểu thức SpEL để lấy trực tiếp courseId (Dùng cho API Tạo mới).
     * Ví dụ: "#request.courseId"
     */
    String courseId() default "";
    
    /**
     * Biểu thức SpEL để lấy ID của thực thể (Dùng cho API Cập nhật / Xóa).
     * Ví dụ: "#id"
     */
    String domainId() default "";
    
    /**
     * Tên của Spring Bean (phải implement CourseIdResolver) dùng để tra cứu database ngầm.
     * Ví dụ: "assignmentResolver"
     */
    String resolver() default "";
}
