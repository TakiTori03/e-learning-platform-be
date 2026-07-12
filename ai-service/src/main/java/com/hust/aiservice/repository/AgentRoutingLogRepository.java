package com.hust.aiservice.repository;

import com.hust.aiservice.entity.AgentRoutingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgentRoutingLogRepository extends JpaRepository<AgentRoutingLog, Long> {
}
