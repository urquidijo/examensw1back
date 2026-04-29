package com.parcial1.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectKpiSettingsResponse {

    private String id;
    private String projectId;

    private int thresholdTickets;
    private int thresholdDays;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}