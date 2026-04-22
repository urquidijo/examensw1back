package com.parcial1.dto.realtime;

import java.util.List;
import java.util.Map;

public record WorkflowRealtimeMessage(
    String workflowId,
    String projectId,
    String clientId,
    List<Map<String, Object>> nodes,
    List<Map<String, Object>> edges
) {}