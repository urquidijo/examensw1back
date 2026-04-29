package com.parcial1.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentKpiResponse {

    private String departmentId;
    private String departmentName;

    private long activeTickets;
    private long delayedTickets;

    private double averageAgeHours;
    private long oldestTicketAgeHours;

    private long uniqueTickets;

    private int thresholdTickets;
    private int thresholdDays;

    private boolean bottleneck;
    private String severity;
    private String message;
}