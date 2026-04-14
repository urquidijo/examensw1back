package com.parcial1.service;

import com.parcial1.dto.AcceptNodeInviteResponse;
import com.parcial1.dto.NodeInviteDetailsResponse;
import com.parcial1.dto.NodeInviteLinkResponse;
import com.parcial1.model.NodeInvite;
import com.parcial1.model.Project;
import com.parcial1.model.ProjectMember;
import com.parcial1.model.ProjectRole;
import com.parcial1.model.User;
import com.parcial1.repository.NodeInviteRepository;
import com.parcial1.repository.ProjectMemberRepository;
import com.parcial1.repository.ProjectRepository;
import com.parcial1.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class NodeInviteService {

    private final NodeInviteRepository nodeInviteRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;

    @Value("${app.frontend-base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    public NodeInviteService(
            NodeInviteRepository nodeInviteRepository,
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            UserRepository userRepository) {
        this.nodeInviteRepository = nodeInviteRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.userRepository = userRepository;
    }

    public NodeInviteLinkResponse createInviteLink(String projectId, String nodeId, String nodeLabel,
            String currentUserEmail) {
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado"));

        boolean isAdminMember = projectMemberRepository.findByProjectIdAndUserId(projectId, currentUser.getId())
                .map(member -> member.getRole() == ProjectRole.ADMINISTRADOR)
                .orElse(false);

        if (!isAdminMember) {
            throw new RuntimeException("No tienes permisos para invitar funcionarios");
        }

        String token = generateUniqueToken();

        NodeInvite invite = NodeInvite.builder()
                .token(token)
                .projectId(projectId)
                .nodeId(nodeId)
                .nodeLabel(nodeLabel != null && !nodeLabel.isBlank() ? nodeLabel : "Nodo")
                .createdByUserId(currentUser.getId())
                .accepted(false)
                .revoked(false)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        nodeInviteRepository.save(invite);

        String inviteLink = frontendBaseUrl + "/node-invite/" + token;

        return NodeInviteLinkResponse.builder()
                .token(token)
                .inviteLink(inviteLink)
                .build();
    }

    public NodeInviteDetailsResponse validateInvite(String token) {
        NodeInvite invite = nodeInviteRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invitación no encontrada"));

        if (invite.isRevoked()) {
            throw new RuntimeException("La invitación fue revocada");
        }

        if (invite.isAccepted()) {
            throw new RuntimeException("La invitación ya fue utilizada");
        }

        if (invite.getExpiresAt() != null && invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("La invitación expiró");
        }

        Project project = projectRepository.findById(invite.getProjectId())
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));

        return NodeInviteDetailsResponse.builder()
                .projectId(project.getId())
                .projectName(project.getName())
                .nodeId(invite.getNodeId())
                .nodeLabel(invite.getNodeLabel())
                .expiresAt(invite.getExpiresAt())
                .build();
    }

    public AcceptNodeInviteResponse acceptInvite(String token, String currentUserEmail) {
        NodeInvite invite = nodeInviteRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invitación no encontrada"));

        if (invite.isRevoked()) {
            throw new RuntimeException("La invitación fue revocada");
        }

        if (invite.isAccepted()) {
            throw new RuntimeException("La invitación ya fue utilizada");
        }

        if (invite.getExpiresAt() != null && invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("La invitación expiró");
        }

        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado"));

        Project project = projectRepository.findById(invite.getProjectId())
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));

        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(project.getId(), user.getId())
                .orElse(
                        ProjectMember.builder()
                                .projectId(project.getId())
                                .userId(user.getId())
                                .joinedAt(LocalDateTime.now())
                                .build());

        member.setRole(ProjectRole.FUNCIONARIO);
        member.setAssignedNodeId(invite.getNodeId());

        if (member.getJoinedAt() == null) {
            member.setJoinedAt(LocalDateTime.now());
        }

        projectMemberRepository.save(member);

        invite.setAccepted(true);
        invite.setAcceptedAt(LocalDateTime.now());
        invite.setAcceptedByUserId(user.getId());

        nodeInviteRepository.save(invite);

        return AcceptNodeInviteResponse.builder()
                .message("Invitación aceptada correctamente")
                .projectId(project.getId())
                .nodeId(invite.getNodeId())
                .build();
    }

    private String generateUniqueToken() {
        String token;
        do {
            token = UUID.randomUUID().toString().replace("-", "")
                    + UUID.randomUUID().toString().replace("-", "");
        } while (nodeInviteRepository.existsByToken(token));
        return token;
    }
}