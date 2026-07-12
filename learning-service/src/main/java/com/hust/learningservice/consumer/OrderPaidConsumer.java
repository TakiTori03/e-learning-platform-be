package com.hust.learningservice.consumer;

import com.hust.commonlibrary.event.OrderPaidEvent;
import com.hust.commonlibrary.constants.KafkaTopics;
import com.hust.commonlibrary.event.EnrollmentSuccessEvent;
import com.hust.learningservice.service.LearningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPaidConsumer {

    private final LearningService learningService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = KafkaTopics.ORDER_PAID, groupId = "learning-group")
    public void consumeOrderPaidEvent(OrderPaidEvent event) {
        log.info("Received OrderPaidEvent from Kafka: {}", event);

        int maxAttempts = 3;
        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxAttempts) {
            attempt++;
            try {
                learningService.enrollStudentBulk(event.getUserId(), event.getCourseIds(), event.getOrderId());
                log.info("Successfully processed enrollment for Order: {} on attempt {}", event.getOrderId(), attempt);

                // Bắn ngược lại event "EnrollmentSuccess" cho order-service để hoàn tất Saga
                EnrollmentSuccessEvent successEvent = EnrollmentSuccessEvent.builder()
                        .orderId(event.getOrderId())
                        .userId(event.getUserId())
                        .status(com.hust.commonlibrary.enums.EnrollmentStatus.SUCCESS)
                        .build();

                kafkaTemplate.send(KafkaTopics.ENROLLMENT_SUCCESS, event.getOrderId(), successEvent);
                log.info("Sent EnrollmentSuccessEvent back to Kafka for Order: {}", event.getOrderId());
                return; // Thoát ngay khi thành công

            } catch (Exception e) {
                lastException = e;
                log.warn("⚠️ Attempt {} of {} failed for Order {}: {}", attempt, maxAttempts, event.getOrderId(), e.getMessage());
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(2000); // Trì hoãn 2 giây trước khi thử lại
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        log.error("❌ Process OrderPaidEvent failed completely after {} attempts for Order {}: {}", maxAttempts, event.getOrderId(), lastException != null ? lastException.getMessage() : "Unknown exception");

        // Bắn tin nhắn báo THẤT BẠI về cho order-service để thực hiện Giao dịch bù (Undo)
        EnrollmentSuccessEvent failEvent = EnrollmentSuccessEvent.builder()
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .status(com.hust.commonlibrary.enums.EnrollmentStatus.FAILED)
                .build();

        try {
            kafkaTemplate.send(KafkaTopics.ENROLLMENT_SUCCESS, event.getOrderId(), failEvent);
            log.info("Sent Enrollment FAILURE callback to order-service for Order: {}", event.getOrderId());
        } catch (Exception kafkaEx) {
            log.error("Critical: Could not even send Failure event to Kafka! {}", kafkaEx.getMessage());
        }
    }
}
