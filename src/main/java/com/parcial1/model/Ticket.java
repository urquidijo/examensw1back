package com.parcial1.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

    @Id
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