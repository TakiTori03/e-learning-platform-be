package com.hust.aiservice.dto.response;

import com.hust.aiservice.agent.enums.AgentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrchestratorResult {
    private String answer;
    private AgentType agentType;
    private Double evaluationScore;
    private Boolean wasCorrected;
    private java.util.List<com.hust.aiservice.dto.Citation> citations;
}
