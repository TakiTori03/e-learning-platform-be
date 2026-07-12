package com.hust.orderservice.listener;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.hust.commonlibrary.constants.FrontendRoutes;
import com.hust.commonlibrary.enums.NotificationType;
import com.hust.commonlibrary.event.NotificationEvent;
import com.hust.orderservice.dto.event.OrderPaidInternalEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderNotificationPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPaid(OrderPaidInternalEvent event) {
        String userId = event.getEvent().getUserId();
        String orderId = event.getEvent().getOrderId();

        NotificationEvent notif = NotificationEvent.builder()
                .userId(userId)
                .title("Thanh toán thành công")
                .message("Đơn hàng #" + orderId + " của bạn đã thanh toán thành công. Các khóa học đã được thêm vào tài khoản.")
                .type(NotificationType.TRANSACTIONAL)
                .redirectUrl(FrontendRoutes.MY_COURSES)
                .build();

        kafkaTemplate.send("notification-events-topic", notif);
        log.info("Sent order notification event to Kafka for order {}", orderId);
    }
}
