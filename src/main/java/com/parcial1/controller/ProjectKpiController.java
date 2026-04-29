package com.parcial1.controller;

import com.parcial1.dto.ProjectKpiResponse;
import com.parcial1.dto.ProjectKpiSettingsRequest;
import com.parcial1.dto.ProjectKpiSettingsResponse;
import com.parcial1.service.ProjectKpiService;
import com.parcial1.service.ProjectKpiSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{projectId}/kpis")
@RequiredArgsConstructor
public class ProjectKpiController {

    private final ProjectKpiService projectKpiService;
    private final ProjectKpiSettingsService projectKpiSettingsService;

    @GetMapping
    public ResponseEntity<ProjectKpiResponse> getProjectKpis(
            @PathVariable String projectId,
            @RequestParam(defaultValue = "10") int thresholdTickets,
            @RequestParam(defaultValue = "2") int thresholdDays
    ) {
        return ResponseEntity.ok(
                projectKpiService.getProjectKpis(projectId, thresholdTickets, thresholdDays)
        );
    }

    @GetMapping("/settings")
    public ResponseEntity<ProjectKpiSettingsResponse> getSettings(
            @PathVariable String projectId
    ) {
        return ResponseEntity.ok(
                projectKpiSettingsService.getSettings(projectId)
        );
    }

    @PutMapping("/settings")
    public ResponseEntity<ProjectKpiSettingsResponse> saveSettings(
            @PathVariable String projectId,
            @RequestBody ProjectKpiSettingsRequest request
    ) {
        return ResponseEntity.ok(
                projectKpiSettingsService.saveSettings(projectId, request)
        );
    }
}