package com.parcial1.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
public class WorkflowDiagramResponse {
    private String workflowId;
    private String projectId;
    private String name;
    private String description;
    private List<Map<String, Object>> nodes;
    private List<Map<String, Object>> edges;
}