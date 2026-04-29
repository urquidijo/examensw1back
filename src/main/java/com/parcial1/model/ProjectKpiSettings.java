package com.parcial1.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "project_kpi_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectKpiSettings {

    @Id
    private String id;

    private String projectId;

    private int thresholdTickets;
    private int thresholdDays;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}