package com.parcial1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class WorkflowDiagramResponse {
    private String projectId;
    private List<Map<String, Object>> nodes;
    private List<Map<String, Object>> edges;
}