package com.parcial1.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketMonitorSummaryResponse {
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Long totalDurationMinutes;

    private List<String> currentDepartments;
    private List<String> currentNodeIds;

    private boolean parallelActive;
}