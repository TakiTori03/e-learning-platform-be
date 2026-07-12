package com.hust.commonlibrary.annotation;

import java.lang.annotation.*;

/**
 * Custom Annotation kích hoạt giới hạn tần suất gọi API (Rate Limiting) thông qua Redis.
 * Giúp chặn spam, tấn công brute-force hoặc D.O.S ở cấp độ microservices.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * Biểu thức SpEL để cá nhân hóa key chặn (ví dụ: "#request.email" hoặc "#userId").
     * Nếu để trống, hệ thống tự động lấy User ID hiện tại hoặc Client IP làm khóa chặn!
     */
    String key() default "";

    /**
     * Số lượng request tối đa được cho phép chạy trong chu kỳ.
     * Ví dụ: 5 (lượt)
     */
    int limit() default 5;

    /**
     * Thời gian hiệu lực của một chu kỳ chặn (giây).
     * Ví dụ: 60 (giây)
     */
    int period() default 60;
}
