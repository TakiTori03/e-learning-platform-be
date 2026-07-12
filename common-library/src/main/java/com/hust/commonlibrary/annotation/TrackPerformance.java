package com.hust.commonlibrary.annotation;

import java.lang.annotation.*;

/**
 * Custom Annotation kích hoạt Giám sát Hiệu năng (Method Latency).
 * Hỗ trợ đo đạc chính xác thời gian phản hồi và phát hiện các tiến trình xử lý chậm (Slow Query/Method).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TrackPerformance {

    /**
     * Ngưỡng thời gian giới hạn chấp nhận được (tính bằng Milliseconds).
     * Mặc định là 1000ms (1 giây). Nếu hàm chạy vượt ngưỡng này, hệ thống sẽ bắn Alert WARN.
     */
    long threshold() default 1000;

    /**
     * Mô tả hoặc nhãn gắn kết để nhận diện nghiệp vụ dễ dàng hơn trong file Log.
     */
    String description() default "";
}
