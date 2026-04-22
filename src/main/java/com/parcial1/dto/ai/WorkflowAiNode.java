package com.parcial1.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkflowAiNode(
    String id,
    String shape,
    Integer x,
    Integer y,
    String label,
    WorkflowAiNodeData data
) {}