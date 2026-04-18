package com.parcial1.dto;

import com.parcial1.model.TicketStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@Builder
public class TicketResponse {
    private String id;
    private String projectId;
    private String workflowId;
    private String workflowName;

    private String title;
    private String description;

    private String clientName;
    private String clientPhone;
    private String clientEmail;
    private String clientReference;

    private TicketStatus status;

    private String currentDepartmentId;
    private String currentDepartmentName;
    private String currentNodeId;

    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Map<String, Object> metadata;
}