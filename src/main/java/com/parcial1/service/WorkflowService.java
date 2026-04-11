package com.parcial1.service;

import com.parcial1.dto.MessageResponse;
import com.parcial1.dto.WorkflowDiagramRequest;
import com.parcial1.dto.WorkflowDiagramResponse;
import com.parcial1.model.ProjectMember;
import com.parcial1.model.ProjectRole;
import com.parcial1.model.User;
import com.parcial1.model.WorkflowDiagram;
import com.parcial1.repository.ProjectMemberRepository;
import com.parcial1.repository.UserRepository;
import com.parcial1.repository.WorkflowDiagramRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final WorkflowDiagramRepository workflowDiagramRepository;
    private final ProjectMemberRepository projectMemberRepository;
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

    public WorkflowDiagramResponse getWorkflow(String projectId) {
        User currentUser = getCurrentUser();

        projectMemberRepository.findByProjectIdAndUserId(projectId, currentUser.getId())
                .orElseThrow(() -> new RuntimeException("No tienes acceso a este proyecto"));

        WorkflowDiagram diagram = workflowDiagramRepository.findByProjectId(projectId)
                .orElse(
                        WorkflowDiagram.builder()
                                .projectId(projectId)
                                .nodes(Collections.emptyList())
                                .edges(Collections.emptyList())
                                .build()
                );

        return WorkflowDiagramResponse.builder()
                .projectId(diagram.getProjectId())
                .nodes(diagram.getNodes())
                .edges(diagram.getEdges())
                .build();
    }

    public MessageResponse saveWorkflow(String projectId, WorkflowDiagramRequest request) {
        User currentUser = getCurrentUser();

        ProjectMember membership = projectMemberRepository.findByProjectIdAndUserId(projectId, currentUser.getId())
                .orElseThrow(() -> new RuntimeException("No perteneces a este proyecto"));

        if (membership.getRole() != ProjectRole.ADMINISTRADOR) {
            throw new RuntimeException("Solo el administrador puede diseñar el flujo");
        }

        WorkflowDiagram diagram = workflowDiagramRepository.findByProjectId(projectId)
                .orElse(WorkflowDiagram.builder().projectId(projectId).build());

        diagram.setProjectId(projectId);
        diagram.setNodes(request.getNodes());
        diagram.setEdges(request.getEdges());

        workflowDiagramRepository.save(diagram);

        return new MessageResponse("Flujo guardado correctamente");
    }
}