package com.parcial1.dto.ai;

import java.util.List;

public record WorkflowAiCommandRequest(
    String prompt,
    String forcedMode,
    WorkflowSnapshotDto workflow,
    List<DepartmentOptionDto> departments
) {}