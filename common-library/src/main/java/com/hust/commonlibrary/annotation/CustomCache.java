package com.hust.commonlibrary.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * Custom Annotation kích hoạt Redis Caching với hỗ trợ cấu hình TTL động (Dynamic TTL)
 * linh hoạt cho từng phương thức (Spring Cache mặc định không hỗ trợ tốt cái này).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CustomCache {

    /**
     * Biểu thức SpEL xác định Cache Key.
     * Ví dụ: "'course:' + #id"
     */
    String key();

    /**
     * Thời gian sống của Cache (Time To Live).
     * Mặc định là 60 phút.
     */
    long ttl() default 60;

    /**
     * Đơn vị thời gian cho TTL.
     * Mặc định là MINUTES (Phút).
     */
    TimeUnit unit() default TimeUnit.MINUTES;
}
