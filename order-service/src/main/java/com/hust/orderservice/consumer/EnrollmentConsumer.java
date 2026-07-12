package com.hust.orderservice.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.hust.commonlibrary.constants.KafkaTopics;
import com.hust.commonlibrary.event.EnrollmentSuccessEvent;
import com.hust.orderservice.constant.OrderStatus;
import com.hust.orderservice.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class EnrollmentConsumer {

    private final OrderRepository orderRepository;

    @KafkaListener(topics = KafkaTopics.ENROLLMENT_SUCCESS, groupId = "order-group")
    @Transactional
    public void consumeEnrollmentSuccess(EnrollmentSuccessEvent event) {
        log.info("Received EnrollmentSuccessEvent for Order ID: {}", event.getOrderId());

        orderRepository.findById(event.getOrderId()).ifPresentOrElse(order -> {
            if (com.hust.commonlibrary.enums.EnrollmentStatus.SUCCESS.equals(event.getStatus())) {
                order.setStatus(OrderStatus.COMPLETED);
                log.info("Saga Completed: Order {} marked as COMPLETED", event.getOrderId());
            } else if (com.hust.commonlibrary.enums.EnrollmentStatus.FAILED.equals(event.getStatus())) {
                order.setStatus(OrderStatus.FAILED);
                log.error("Saga FAILED: Order {} marked as FAILED. Manual intervention or refund needed.", event.getOrderId());
            }
            orderRepository.save(order);
        }, () -> log.error("Order {} not found for Saga completion!", event.getOrderId()));
    }
}
