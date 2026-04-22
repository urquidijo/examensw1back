package com.parcial1.dto.ai;

import java.util.List;
import java.util.Map;

public record WorkflowAiOperation(
    String type,
    String alias,
    String target,
    String source,
    String nodeType,
    String label,
    String newLabel,
    Boolean reconnect,
    String departmentName,
    String instructions,
    String decisionQuestion,
    List<Map<String, String>> decisionOptions,
    String conditionValue,
    String conditionLabel,
    Integer x,
    Integer y
) {}