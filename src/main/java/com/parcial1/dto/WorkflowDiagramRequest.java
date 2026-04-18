package com.parcial1.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class WorkflowDiagramRequest {
    private List<Map<String, Object>> nodes;
    private List<Map<String, Object>> edges;
}