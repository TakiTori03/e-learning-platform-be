package com.hust.orderservice.repository;

import com.hust.orderservice.entity.OutboxEvent;
import com.hust.orderservice.constant.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")}) // -2 means SKIP LOCKED in Hibernate for PostgreSQL
    @Query("SELECT o FROM OutboxEvent o WHERE o.status = :status")
    List<OutboxEvent> findByStatusForUpdate(@Param("status") OutboxStatus status, org.springframework.data.domain.Pageable pageable);

    List<OutboxEvent> findByStatus(OutboxStatus status);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("DELETE FROM OutboxEvent o WHERE o.status = :status AND o.createdAt < :cutoff")
    void deleteByStatusAndCreatedAtBefore(OutboxStatus status, java.time.Instant cutoff);
}

