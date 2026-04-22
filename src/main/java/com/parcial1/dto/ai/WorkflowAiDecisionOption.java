package com.parcial1.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkflowAiDecisionOption(
    String value,
    String label
) {}