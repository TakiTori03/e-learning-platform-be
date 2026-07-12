package com.hust.commonlibrary.annotation;

import java.lang.annotation.*;

/**
 * Custom Annotation kích hoạt việc Xóa Cache (Cache Eviction) trong Redis
 * khi dữ liệu gốc bị sửa đổi hoặc xóa bỏ, tránh kịch bản dữ liệu bị cũ (stale).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CustomCacheEvict {

    /**
     * Biểu thức SpEL để xác định Cache Key cần xóa.
     * Ví dụ: "'course:detail:' + #id"
     */
    String key();
}
