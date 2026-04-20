package com.parcial1.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketMonitorStepResponse {
    private String kind; // COMPLETED | CURRENT
    private String nodeId;
    private String nodeLabel;
    private String nodeType;

    private String departmentId;
    private String departmentName;

    private String assignedUserId;
    private String assignedUserName;

    private String decisionResult;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    private Long durationMinutes;

    private String parallelGroupId;
}