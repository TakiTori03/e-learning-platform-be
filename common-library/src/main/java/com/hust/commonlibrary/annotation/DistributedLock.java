package com.hust.commonlibrary.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * Custom Annotation kích hoạt Khóa Phân Tán (Distributed Lock) toàn hệ thống.
 * Đảm bảo chống Race Condition, Mua trùng lặp trong môi trường Clustering Microservices.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {

    /**
     * Cú pháp SpEL định nghĩa key của lock trong Redis.
     * Ví dụ: "'buy:' + #request.courseId"
     */
    String key();

    /**
     * Thời gian tối đa chờ đợi để lấy được khóa (giây).
     * Nếu vượt quá thời gian này, throw Exception báo bận.
     */
    long waitTime() default 5;

    /**
     * Thời gian khóa tự động hết hạn (Lease Time) (giây).
     * Tránh kịch bản Server crash làm kẹt khóa vĩnh viễn.
     */
    long leaseTime() default 10;

    /**
     * Đơn vị thời gian. Mặc định là GIÂY (SECONDS).
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
