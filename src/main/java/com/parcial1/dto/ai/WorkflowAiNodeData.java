package com.parcial1.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkflowAiNodeData(
    String label,
    String nodeType,
    String departmentId,
    String departmentName,
    String instructions,
    String aiAlias,
    String decisionMode,
    String decisionQuestion,
    List<WorkflowAiDecisionOption> decisionOptions
) {
    public WorkflowAiNodeData {
        decisionOptions = decisionOptions == null ? List.of() : List.copyOf(decisionOptions);
    }
}