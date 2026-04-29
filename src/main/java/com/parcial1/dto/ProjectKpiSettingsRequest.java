package com.parcial1.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectKpiSettingsRequest {

    private int thresholdTickets;
    private int thresholdDays;
}