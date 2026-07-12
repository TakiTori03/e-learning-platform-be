package com.hust.commonlibrary.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * Custom Annotation kích hoạt tính năng Chống trùng lặp yêu cầu (Idempotency) phân tán.
 * Ngăn chặn tình trạng Double-Click hoặc spam dữ liệu gây lỗi thanh toán, tạo đơn hàng, v.v.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * Biểu thức SpEL để xác định Khóa lũy đẳng độc nhất.
     * Ví dụ: "'create_order:' + #request.orderCode"
     */
    String key();

    /**
     * Thời gian duy trì khóa chặn (ngăn chặn trùng lặp).
     * Mặc định là 3 giây.
     */
    long expireTime() default 3;

    /**
     * Đơn vị thời gian hiệu lực.
     */
    TimeUnit unit() default TimeUnit.SECONDS;
}
