package com.parcial1.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Document(collection = "workflow_diagrams")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowDiagram {

    @Id
    private String id;

    private String projectId;
    private List<Map<String, Object>> nodes;
    private List<Map<String, Object>> edges;
}