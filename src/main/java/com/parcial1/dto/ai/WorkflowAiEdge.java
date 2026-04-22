package com.parcial1.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkflowAiEdge(
    String id,
    String shape,
    WorkflowAiEdgeEndpoint source,
    WorkflowAiEdgeEndpoint target,
    Map<String, Object> attrs
) {}