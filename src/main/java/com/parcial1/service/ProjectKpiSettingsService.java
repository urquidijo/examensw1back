package com.parcial1.service;

import com.parcial1.dto.ProjectKpiSettingsRequest;
import com.parcial1.dto.ProjectKpiSettingsResponse;
import com.parcial1.model.ProjectKpiSettings;
import com.parcial1.repository.ProjectKpiSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProjectKpiSettingsService {

    private final ProjectKpiSettingsRepository projectKpiSettingsRepository;

    public ProjectKpiSettingsResponse getSettings(String projectId) {
        ProjectKpiSettings settings = projectKpiSettingsRepository
                .findByProjectId(projectId)
                .orElseGet(() -> createDefaultSettings(projectId));

        return mapResponse(settings);
    }

    public ProjectKpiSettingsResponse saveSettings(String projectId, ProjectKpiSettingsRequest request) {
        LocalDateTime now = LocalDateTime.now();

        int thresholdTickets = Math.max(1, request.getThresholdTickets());
        int thresholdDays = Math.max(1, request.getThresholdDays());

        ProjectKpiSettings settings = projectKpiSettingsRepository
                .findByProjectId(projectId)
                .orElseGet(() -> ProjectKpiSettings.builder()
                        .projectId(projectId)
                        .createdAt(now)
                        .build());

        settings.setThresholdTickets(thresholdTickets);
        settings.setThresholdDays(thresholdDays);
        settings.setUpdatedAt(now);

        ProjectKpiSettings saved = projectKpiSettingsRepository.save(settings);

        return mapResponse(saved);
    }

    private ProjectKpiSettings createDefaultSettings(String projectId) {
        LocalDateTime now = LocalDateTime.now();

        ProjectKpiSettings settings = ProjectKpiSettings.builder()
                .projectId(projectId)
                .thresholdTickets(10)
                .thresholdDays(2)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return projectKpiSettingsRepository.save(settings);
    }

    private ProjectKpiSettingsResponse mapResponse(ProjectKpiSettings settings) {
        return ProjectKpiSettingsResponse.builder()
                .id(settings.getId())
                .projectId(settings.getProjectId())
                .thresholdTickets(settings.getThresholdTickets())
                .thresholdDays(settings.getThresholdDays())
                .createdAt(settings.getCreatedAt())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }
}