package com.parcial1.controller;

import com.parcial1.dto.CreateProjectRequest;
import com.parcial1.dto.InviteUserRequest;
import com.parcial1.dto.MessageResponse;
import com.parcial1.dto.ProjectSummaryResponse;
import com.parcial1.model.Project;
import com.parcial1.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<Project> createProject(@Valid @RequestBody CreateProjectRequest request) {
        return ResponseEntity.ok(projectService.createProject(request));
    }

    @GetMapping("/me")
    public ResponseEntity<List<ProjectSummaryResponse>> getMyProjects() {
        return ResponseEntity.ok(projectService.getMyProjects());
    }

    @GetMapping("/owned")
    public ResponseEntity<List<ProjectSummaryResponse>> getOwnedProjects() {
        return ResponseEntity.ok(projectService.getOwnedProjects());
    }

    @GetMapping("/shared")
    public ResponseEntity<List<ProjectSummaryResponse>> getSharedProjects() {
        return ResponseEntity.ok(projectService.getSharedProjects());
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<Project> getProjectById(@PathVariable String projectId) {
        return ResponseEntity.ok(projectService.getProjectById(projectId));
    }

    @PostMapping("/{projectId}/invite")
    public ResponseEntity<MessageResponse> inviteUser(
            @PathVariable String projectId,
            @Valid @RequestBody InviteUserRequest request) {
        projectService.inviteUser(projectId, request);
        return ResponseEntity.ok(new MessageResponse("Usuario invitado correctamente"));
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<MessageResponse> deleteProject(@PathVariable String projectId) {
        projectService.deleteProject(projectId);
        return ResponseEntity.ok(new MessageResponse("Proyecto eliminado correctamente"));
    }
}