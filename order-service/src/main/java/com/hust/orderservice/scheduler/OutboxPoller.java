package com.hust.orderservice.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hust.commonlibrary.constants.KafkaTopics;
import com.hust.commonlibrary.event.OrderPaidEvent;
import com.hust.orderservice.constant.OutboxStatus;
import com.hust.orderservice.entity.OutboxEvent;
import com.hust.orderservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import org.springframework.data.domain.PageRequest;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPoller {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final PlatformTransactionManager transactionManager;

    @Scheduled(fixedDelay = 3000)
    public void pollOutboxEvents() {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        // 1. Fetch pending events (limited to 100 per run) and transition to PROCESSING in a short transaction (releases DB locks quickly)
        List<OutboxEvent> eventsToProcess = txTemplate.execute(status -> {
            List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatusForUpdate(
                    OutboxStatus.PENDING,
                    PageRequest.of(0, 100)
            );
            if (pendingEvents.isEmpty()) {
                return Collections.emptyList();
            }
            for (OutboxEvent event : pendingEvents) {
                event.setStatus(OutboxStatus.PROCESSING);
            }
            return outboxEventRepository.saveAll(pendingEvents);
        });

        if (eventsToProcess == null || eventsToProcess.isEmpty()) {
            return;
        }

        log.info("Outbox Poller: Found {} pending events to publish", eventsToProcess.size());

        for (OutboxEvent event : eventsToProcess) {
            try {
                if ("ORDER_PAID".equals(event.getEventType())) {
                    OrderPaidEvent orderPaidEvent = objectMapper.readValue(event.getPayload(), OrderPaidEvent.class);

                    log.info("Outbox Poller: Publishing OrderPaidEvent to Kafka for Order ID: {}", event.getAggregateId());

                    // Synchronously get to guarantee delivery (network call happens OUTSIDE DB transaction)
                    kafkaTemplate.send(KafkaTopics.ORDER_PAID, event.getAggregateId(), orderPaidEvent).get();

                    // 2. Commit success state in a new short transaction
                    txTemplate.executeWithoutResult(status -> {
                        OutboxEvent dbEvent = outboxEventRepository.findById(event.getId()).orElse(event);
                        dbEvent.setStatus(OutboxStatus.PROCESSED);
                        dbEvent.setProcessedAt(Instant.now());
                        outboxEventRepository.save(dbEvent);
                    });
                    log.info("Outbox Poller: Successfully published and updated status to PROCESSED for Event ID: {}", event.getId());
                }
            } catch (Exception e) {
                log.error("Outbox Poller: Failed to process OutboxEvent ID: {}. Error: {}", event.getId(), e.getMessage());

                // 3. Rollback status to PENDING or set to FAILED in a new short transaction
                txTemplate.executeWithoutResult(status -> {
                    OutboxEvent dbEvent = outboxEventRepository.findById(event.getId()).orElse(event);
                    int retries = dbEvent.getRetryCount() + 1;
                    dbEvent.setRetryCount(retries);
                    if (retries >= 5) {
                        dbEvent.setStatus(OutboxStatus.FAILED);
                        log.error("Outbox Poller: Event ID {} exceeded max retries. Marked as FAILED.", dbEvent.getId());
                    } else {
                        dbEvent.setStatus(OutboxStatus.PENDING);
                    }
                    outboxEventRepository.save(dbEvent);
                });
            }
        }
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void cleanOldProcessedEvents() {
        log.info("Outbox Cleanup: Starting daily cleanup of old processed events");
        try {
            Instant cutoff = Instant.now().minus(7, java.time.temporal.ChronoUnit.DAYS);
            outboxEventRepository.deleteByStatusAndCreatedAtBefore(OutboxStatus.PROCESSED, cutoff);
            log.info("Outbox Cleanup: Completed successfully for events older than {}", cutoff);
        } catch (Exception e) {
            log.error("Outbox Cleanup: Failed to execute cleanup. Error: {}", e.getMessage());
        }
    }
}
