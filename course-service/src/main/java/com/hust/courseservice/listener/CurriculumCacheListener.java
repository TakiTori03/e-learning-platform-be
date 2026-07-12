package com.hust.courseservice.listener;

import com.hust.commonlibrary.service.RedisService;
import com.hust.courseservice.event.CurriculumMutatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class CurriculumCacheListener {

    private final RedisService redisService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleCurriculumMutation(CurriculumMutatedEvent event) {
        if (event.getCourseId() != null) {
            try {
                String cacheKey = "course:detail:" + event.getCourseId();
                redisService.delete(cacheKey);
                log.info("Successfully evicted course detail cache via Event-Driven pattern for courseId: {}", event.getCourseId());
            } catch (Exception e) {
                log.error("Failed to evict cache for course {} via event: {}", event.getCourseId(), e.getMessage());
            }
        }
    }
}
