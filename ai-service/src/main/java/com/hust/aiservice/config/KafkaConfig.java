package com.hust.aiservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
@Slf4j
public class KafkaConfig {

    @Value("${spring.kafka.consumer.attempts:3}")
    private int maxAttempts;

    @Value("${spring.kafka.consumer.backoff-delay:2000}")
    private long backoffInterval;

    @Value("${spring.kafka.consumer.backoff-multiplier:2.0}")
    private double backoffMultiplier;

    /**
     * Cấu hình cơ chế tự động thử lại (Retry) cho Kafka Listener khi gặp lỗi (ví dụ lỗi gọi API Gemini, lỗi mạng tạm thời).
     * Spring Kafka sẽ tự động bắt CommonErrorHandler này và áp dụng cho các Listener Factory.
     */
    @Bean
    public CommonErrorHandler kafkaErrorHandler() {
        log.info("⚙️ Khởi tạo Kafka CommonErrorHandler: maxAttempts={}, backoffInterval={}ms, multiplier={}", 
                maxAttempts, backoffInterval, backoffMultiplier);

        // Thiết lập Exponential Backoff
        ExponentialBackOff backOff = new ExponentialBackOff(backoffInterval, backoffMultiplier);
        backOff.setMaxAttempts(maxAttempts);

        // Tránh thử lại đối với các lỗi không thể khắc phục bằng cách thử lại (ví dụ: lỗi cú pháp SQL, định dạng dữ liệu lỗi)
        DefaultErrorHandler errorHandler = new DefaultErrorHandler((record, exception) -> {
            log.error("❌ Kafka Consumer đã thử lại hết số lần tối đa ({}) mà vẫn lỗi. Discarding records. Exception: {}", 
                    maxAttempts, exception.getMessage());
        }, backOff);

        // Các ngoại lệ không muốn thử lại (ví dụ NullPointerException, IllegalArgumentException, SQL State lỗi cú pháp)
        errorHandler.addNotRetryableExceptions(
                NullPointerException.class,
                IllegalArgumentException.class
        );

        return errorHandler;
    }

}
