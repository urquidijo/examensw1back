package com.parcial1.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkflowAiGraphResponse(
    String mode,
    String summary,
    List<WorkflowAiNode> nodes,
    List<WorkflowAiEdge> edges
) {
    public WorkflowAiGraphResponse {
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
        edges = edges == null ? List.of() : List.copyOf(edges);
    }
}