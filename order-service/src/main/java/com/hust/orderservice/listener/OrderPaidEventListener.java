package com.hust.orderservice.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hust.orderservice.constant.OutboxStatus;
import com.hust.orderservice.dto.event.OrderPaidInternalEvent;
import com.hust.orderservice.entity.OutboxEvent;
import com.hust.orderservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPaidEventListener {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @EventListener
    public void handleOrderPaidEvent(OrderPaidInternalEvent internalEvent) {
        String orderId = internalEvent.getEvent().getOrderId();
        log.info("Outbox Pattern: Persisting OrderPaidEvent to database for Order ID: {}", orderId);

        try {
            String payloadJson = objectMapper.writeValueAsString(internalEvent.getEvent());

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType("Order")
                    .aggregateId(orderId)
                    .eventType("ORDER_PAID")
                    .payload(payloadJson)
                    .status(OutboxStatus.PENDING)
                    .retryCount(0)
                    .createdAt(Instant.now())
                    .build();

            outboxEventRepository.save(outboxEvent);
            log.info("Outbox Pattern: Successfully saved pending OutboxEvent for Order ID: {}", orderId);

        } catch (Exception e) {
            log.error("Outbox Pattern: Failed to persist OutboxEvent for Order {}: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Failed to persist OutboxEvent for Order " + orderId, e);
        }
    }
}
