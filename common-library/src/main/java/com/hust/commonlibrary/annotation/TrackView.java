package com.hust.commonlibrary.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TrackView {

    /**
     * Loại thực thể cần tăng view (ví dụ: "blog", "course", "lesson",...)
     */
    String type() default "blog";

    /**
     * Biểu thức SpEL để lấy trực tiếp ID đối tượng từ method parameters.
     * Ví dụ: "#id", "#blogId" hoặc "#request.id"
     */
    String value() default "#id";
}
