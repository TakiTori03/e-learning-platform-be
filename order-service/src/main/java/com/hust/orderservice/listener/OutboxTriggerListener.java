package com.hust.orderservice.listener;

import com.hust.orderservice.dto.event.OutboxAddedEvent;
import com.hust.orderservice.scheduler.OutboxPoller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxTriggerListener {

    private final OutboxPoller outboxPoller;

    /**
     * Lắng nghe sự kiện bản ghi Outbox mới được lưu.
     * Chỉ chạy SAU KHI Database Transaction chính (tạo đơn hàng / xác nhận VNPAY) commit thành công.
     * Chạy bất đồng bộ (@Async) trên luồng phụ để tránh block luồng xử lý request chính của user.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleOutboxAdded(OutboxAddedEvent event) {
        log.info("Outbox Hybrid Trigger: DB Transaction committed. Waking up poller immediately.");
        try {
            outboxPoller.pollOutboxEvents();
        } catch (Exception e) {
            log.error("Outbox Hybrid Trigger: Failed to trigger outbox poller", e);
        }
    }
}
