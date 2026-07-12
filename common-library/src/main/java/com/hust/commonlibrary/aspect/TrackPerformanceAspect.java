package com.hust.commonlibrary.aspect;

import com.hust.commonlibrary.annotation.TrackPerformance;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

/**
 * Aspect Giám sát Hiệu năng Tốc độ Phương thức.
 * Sử dụng cơ chế StopWatch để đo đạc chính xác tới từng micro-giây mà không tốn chi phí vận hành lớn.
 */
@Aspect
@Component
@Slf4j
public class TrackPerformanceAspect {

    @Around("@annotation(trackPerformance)")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint, TrackPerformance trackPerformance) throws Throwable {
        // Khởi tạo bộ bấm giờ độ chính xác cao của Spring
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
            // Thực thi phương thức gốc
            return joinPoint.proceed();
        } finally {
            stopWatch.stop();
            long executionTime = stopWatch.getTotalTimeMillis();

            // Thu thập thông tin phương thức để định danh
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String className = signature.getDeclaringType().getSimpleName();
            String methodName = signature.getName();
            
            // Ưu tiên sử dụng mô tả nghiệp vụ nếu được cấu hình
            String displayName = trackPerformance.description().isBlank() 
                    ? className + "#" + methodName 
                    : trackPerformance.description();

            // 🚨 PHÁT HIỆN QUÁ NGƯỠNG (Threshold Breach)
            if (executionTime >= trackPerformance.threshold()) {
                log.warn("🚨 [SLOW METHOD WARNING] Target: [{}] took {}ms to execute. Threshold SLA: {}ms.", 
                        displayName, executionTime, trackPerformance.threshold());
            } else {
                // Trạng thái bình thường -> Log nhẹ nhàng ở mức độ DEBUG để tránh làm loãng log console
                log.debug("⏱️ [Performance Trace] Target: [{}] finished in {}ms.", displayName, executionTime);
            }
        }
    }
}
