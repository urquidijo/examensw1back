package com.parcial1.dto.ai;

import java.util.List;

public record WorkflowAiResponse(
    String mode,
    String summary,
    List<WorkflowAiOperation> operations
) {}