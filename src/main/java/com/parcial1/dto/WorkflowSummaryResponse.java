package com.parcial1.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class WorkflowSummaryResponse {
    private String id;
    private String projectId;
    private String name;
    private String description;
    private int nodesCount;
    private int edgesCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}