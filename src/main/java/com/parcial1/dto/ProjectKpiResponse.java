package com.parcial1.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectKpiResponse {

    private String projectId;
    private LocalDateTime generatedAt;

    private long totalActiveTickets;
    private long totalDelayedTickets;
    private long totalBottleneckDepartments;

    private int thresholdTickets;
    private int thresholdDays;

    private List<DepartmentKpiResponse> departments;
}