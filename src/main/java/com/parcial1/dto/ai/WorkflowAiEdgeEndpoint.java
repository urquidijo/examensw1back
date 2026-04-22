package com.parcial1.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkflowAiEdgeEndpoint(
    String cell,
    String port
) {}