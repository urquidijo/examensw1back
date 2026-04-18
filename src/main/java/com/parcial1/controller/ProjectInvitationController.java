package com.parcial1.controller;

import com.parcial1.dto.CreateProjectInvitationRequest;
import com.parcial1.dto.MessageResponse;
import com.parcial1.dto.ProjectInvitationResponse;
import com.parcial1.dto.ProjectMemberResponse;
import com.parcial1.service.ProjectInvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ProjectInvitationController {

    private final ProjectInvitationService projectInvitationService;

    @GetMapping("/projects/{projectId}/members")
    public ResponseEntity<List<ProjectMemberResponse>> getProjectMembers(@PathVariable String projectId) {
        return ResponseEntity.ok(projectInvitationService.getProjectMembers(projectId));
    }

    @GetMapping("/projects/{projectId}/invitations")
    public ResponseEntity<List<ProjectInvitationResponse>> getProjectInvitations(@PathVariable String projectId) {
        return ResponseEntity.ok(projectInvitationService.getProjectInvitations(projectId));
    }

    @PostMapping("/projects/{projectId}/invitations")
    public ResponseEntity<MessageResponse> createInvitation(
            @PathVariable String projectId,
            @Valid @RequestBody CreateProjectInvitationRequest request
    ) {
        return ResponseEntity.ok(projectInvitationService.createInvitation(projectId, request));
    }

    @GetMapping("/project-invitations/me")
    public ResponseEntity<List<ProjectInvitationResponse>> getMyPendingInvitations() {
        return ResponseEntity.ok(projectInvitationService.getMyPendingInvitations());
    }

    @PostMapping("/project-invitations/{invitationId}/accept")
    public ResponseEntity<MessageResponse> acceptInvitation(@PathVariable String invitationId) {
        return ResponseEntity.ok(projectInvitationService.acceptInvitation(invitationId));
    }

    @PostMapping("/project-invitations/{invitationId}/reject")
    public ResponseEntity<MessageResponse> rejectInvitation(@PathVariable String invitationId) {
        return ResponseEntity.ok(projectInvitationService.rejectInvitation(invitationId));
    }
}