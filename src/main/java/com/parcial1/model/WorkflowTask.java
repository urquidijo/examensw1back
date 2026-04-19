package com.parcial1.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = "workflow_tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowTask {

    @Id
    private String id;

    private String projectId;
    private String ticketId;

    private String workflowId;
    private String nodeId;
    private String nodeLabel;
    private String nodeType;

    private String departmentId;
    private String departmentName;

    private String assignedUserId;
    private String assignedUserName;

    private boolean requiresTramite;
    private String tramiteTemplateId;
    private String tramiteTemplateName;

    private List<StoredFileInfo> uploadedFiles;

    private String decisionMode;
    private String decisionQuestion;
    private List<Map<String, String>> decisionOptions;
    

    private TaskStatus status;

    private Map<String, Object> submittedTramiteData;

    private String parallelGroupId;
    private String forkNodeId;
    private String joinNodeId;
    private String branchSourceNodeId;

    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}