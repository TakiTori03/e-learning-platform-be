package com.hust.aiservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "agent_routing_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRoutingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", length = 36, nullable = false)
    private String sessionId;

    @Column(name = "user_query", nullable = false, columnDefinition = "TEXT")
    private String userQuery;

    @Column(name = "routed_agent", length = 30, nullable = false)
    private String routedAgent;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "evaluation_score")
    private Double evaluationScore;

    @Column(name = "was_corrected")
    private Boolean wasCorrected;

    @Column(name = "correction_source", length = 50)
    private String correctionSource;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
