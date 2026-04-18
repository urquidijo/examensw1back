package com.parcial1.service;

import com.parcial1.dto.MessageResponse;
import com.parcial1.dto.CreateProjectInvitationRequest;
import com.parcial1.dto.ProjectInvitationResponse;
import com.parcial1.dto.ProjectMemberResponse;
import com.parcial1.model.*;
import com.parcial1.repository.ProjectInvitationRepository;
import com.parcial1.repository.ProjectMemberRepository;
import com.parcial1.repository.ProjectRepository;
import com.parcial1.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectInvitationService {

    private final ProjectInvitationRepository projectInvitationRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Usuario no autenticado");
        }

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado"));
    }

    private ProjectMember getMembership(String projectId, String userId) {
        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new RuntimeException("No perteneces a este proyecto"));
    }

    private void validateAdmin(ProjectMember membership) {
        if (membership.getRole() != ProjectRole.ADMINISTRADOR) {
            throw new RuntimeException("Solo el administrador puede gestionar usuarios");
        }
    }

    public List<ProjectMemberResponse> getProjectMembers(String projectId) {
        User currentUser = getCurrentUser();
        getMembership(projectId, currentUser.getId());

        List<ProjectMember> members = projectMemberRepository.findByProjectId(projectId);

        return members.stream().map(member -> {
            User user = userRepository.findById(member.getUserId()).orElse(null);

            return ProjectMemberResponse.builder()
                    .userId(member.getUserId())
                    .name(user != null ? user.getName() : "Usuario")
                    .email(user != null ? user.getEmail() : "")
                    .role(member.getRole())
                    .assignedNodeId(member.getAssignedNodeId())
                    .build();
        }).toList();
    }

    public List<ProjectInvitationResponse> getProjectInvitations(String projectId) {
        User currentUser = getCurrentUser();
        ProjectMember membership = getMembership(projectId, currentUser.getId());
        validateAdmin(membership);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));

        return projectInvitationRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(invitation -> ProjectInvitationResponse.builder()
                        .id(invitation.getId())
                        .projectId(invitation.getProjectId())
                        .projectName(project.getName())
                        .invitedUserId(invitation.getInvitedUserId())
                        .invitedName(invitation.getInvitedName())
                        .invitedEmail(invitation.getInvitedEmail())
                        .role(invitation.getRole())
                        .status(invitation.getStatus())
                        .createdAt(invitation.getCreatedAt())
                        .build())
                .toList();
    }

    public MessageResponse createInvitation(String projectId, CreateProjectInvitationRequest request) {
        User currentUser = getCurrentUser();

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));

        ProjectMember membership = getMembership(projectId, currentUser.getId());
        validateAdmin(membership);

        String email = request.getEmail() != null ? request.getEmail().trim() : "";
        String name = request.getName() != null ? request.getName().trim() : "";

        if (email.isBlank() && name.isBlank()) {
            throw new RuntimeException("Debes enviar email o nombre del usuario");
        }

        User invitedUser = null;

        if (!email.isBlank()) {
            invitedUser = userRepository.findByEmail(email).orElse(null);
        }

        if (invitedUser == null && !name.isBlank()) {
            invitedUser = userRepository.findAll().stream()
                    .filter(user -> user.getName() != null && user.getName().equalsIgnoreCase(name))
                    .findFirst()
                    .orElse(null);
        }

        if (invitedUser == null) {
            throw new RuntimeException("El usuario invitado no existe");
        }

        if (!invitedUser.isActive()) {
            throw new RuntimeException("El usuario invitado está inactivo");
        }

        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, invitedUser.getId())) {
            throw new RuntimeException("El usuario ya pertenece a este proyecto");
        }

        boolean pendingInvitationExists = projectInvitationRepository
                .existsByProjectIdAndInvitedUserIdAndStatus(
                        projectId,
                        invitedUser.getId(),
                        InvitationStatus.PENDIENTE
                );

        if (pendingInvitationExists) {
            throw new RuntimeException("Ya existe una invitación pendiente para este usuario");
        }

        ProjectInvitation invitation = ProjectInvitation.builder()
                .projectId(project.getId())
                .invitedUserId(invitedUser.getId())
                .invitedName(invitedUser.getName())
                .invitedEmail(invitedUser.getEmail())
                .role(request.getRole())
                .invitedByUserId(currentUser.getId())
                .status(InvitationStatus.PENDIENTE)
                .createdAt(LocalDateTime.now())
                .build();

        projectInvitationRepository.save(invitation);

        return new MessageResponse("Invitación enviada correctamente");
    }

    public List<ProjectInvitationResponse> getMyPendingInvitations() {
        User currentUser = getCurrentUser();

        return projectInvitationRepository
                .findByInvitedUserIdAndStatusOrderByCreatedAtDesc(
                        currentUser.getId(),
                        InvitationStatus.PENDIENTE
                )
                .stream()
                .map(invitation -> {
                    Project project = projectRepository.findById(invitation.getProjectId()).orElse(null);

                    return ProjectInvitationResponse.builder()
                            .id(invitation.getId())
                            .projectId(invitation.getProjectId())
                            .projectName(project != null ? project.getName() : "Proyecto")
                            .invitedUserId(invitation.getInvitedUserId())
                            .invitedName(invitation.getInvitedName())
                            .invitedEmail(invitation.getInvitedEmail())
                            .role(invitation.getRole())
                            .status(invitation.getStatus())
                            .createdAt(invitation.getCreatedAt())
                            .build();
                })
                .toList();
    }

    public MessageResponse acceptInvitation(String invitationId) {
        User currentUser = getCurrentUser();

        ProjectInvitation invitation = projectInvitationRepository
                .findByIdAndInvitedUserId(invitationId, currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Invitación no encontrada"));

        if (invitation.getStatus() != InvitationStatus.PENDIENTE) {
            throw new RuntimeException("La invitación ya fue procesada");
        }

        if (projectMemberRepository.existsByProjectIdAndUserId(invitation.getProjectId(), currentUser.getId())) {
            throw new RuntimeException("Ya perteneces a este proyecto");
        }

        ProjectMember member = ProjectMember.builder()
                .projectId(invitation.getProjectId())
                .userId(currentUser.getId())
                .role(invitation.getRole())
                .joinedAt(LocalDateTime.now())
                .build();

        projectMemberRepository.save(member);

        invitation.setStatus(InvitationStatus.ACEPTADA);
        invitation.setRespondedAt(LocalDateTime.now());
        projectInvitationRepository.save(invitation);

        return new MessageResponse("Invitación aceptada correctamente");
    }

    public MessageResponse rejectInvitation(String invitationId) {
        User currentUser = getCurrentUser();

        ProjectInvitation invitation = projectInvitationRepository
                .findByIdAndInvitedUserId(invitationId, currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Invitación no encontrada"));

        if (invitation.getStatus() != InvitationStatus.PENDIENTE) {
            throw new RuntimeException("La invitación ya fue procesada");
        }

        invitation.setStatus(InvitationStatus.RECHAZADA);
        invitation.setRespondedAt(LocalDateTime.now());
        projectInvitationRepository.save(invitation);

        return new MessageResponse("Invitación rechazada correctamente");
    }
}