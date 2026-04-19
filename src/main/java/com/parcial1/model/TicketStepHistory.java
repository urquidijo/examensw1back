package com.parcial1.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "ticket_step_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketStepHistory {

    @Id
    private String id;

    private String projectId;
    private String ticketId;
    private String workflowId;

    private String nodeId;
    private String nodeLabel;

    private String departmentId;
    private String departmentName;

    private String assignedUserId;
    private String assignedUserName;

    private boolean requiresTramite;
    private String tramiteTemplateId;
    private String tramiteTemplateName;

    private Map<String, Object> submittedTramiteData;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}