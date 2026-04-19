package com.parcial1.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

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

    private String decisionResult;
    private Map<String, Object> submittedTramiteData;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}