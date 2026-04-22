package com.parcial1.dto.ai;

import java.util.List;
import java.util.Map;

public record WorkflowSnapshotDto(
    List<Map<String, Object>> nodes,
    List<Map<String, Object>> edges
) {}