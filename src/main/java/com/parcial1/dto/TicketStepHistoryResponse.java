package com.parcial1.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.parcial1.model.StoredFileInfo;

@Getter
@Setter
@Builder
public class TicketStepHistoryResponse {
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

    private String decisionResult;
    private Map<String, Object> submittedTramiteData;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}