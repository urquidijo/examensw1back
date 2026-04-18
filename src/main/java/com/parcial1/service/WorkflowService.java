package com.parcial1.service;

import com.parcial1.dto.CreateWorkflowRequest;
import com.parcial1.dto.MessageResponse;
import com.parcial1.dto.WorkflowDiagramRequest;
import com.parcial1.dto.WorkflowDiagramResponse;
import com.parcial1.dto.WorkflowSummaryResponse;
import com.parcial1.model.ProjectMember;
import com.parcial1.model.ProjectRole;
import com.parcial1.model.User;
import com.parcial1.model.Workflow;
import com.parcial1.repository.ProjectMemberRepository;
import com.parcial1.repository.UserRepository;
import com.parcial1.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
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

    private ProjectMember getMembership(String projectId, String userId) {
        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new RuntimeException("No tienes acceso a este proyecto"));
    }

    private void validateAdmin(ProjectMember membership) {
        if (membership.getRole() != ProjectRole.ADMINISTRADOR) {
            throw new RuntimeException("Solo el administrador puede gestionar workflows");
        }
    }

    public List<WorkflowSummaryResponse> getWorkflows(String projectId) {
        User currentUser = getCurrentUser();
        getMembership(projectId, currentUser.getId());

        return workflowRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(workflow -> WorkflowSummaryResponse.builder()
                        .id(workflow.getId())
                        .projectId(workflow.getProjectId())
                        .name(workflow.getName())
                        .description(workflow.getDescription())
                        .nodesCount(workflow.getNodes() != null ? workflow.getNodes().size() : 0)
                        .edgesCount(workflow.getEdges() != null ? workflow.getEdges().size() : 0)
                        .createdAt(workflow.getCreatedAt())
                        .updatedAt(workflow.getUpdatedAt())
                        .build())
                .toList();
    }

    public WorkflowSummaryResponse createWorkflow(String projectId, CreateWorkflowRequest request) {
        User currentUser = getCurrentUser();
        ProjectMember membership = getMembership(projectId, currentUser.getId());
        validateAdmin(membership);

        Workflow workflow = Workflow.builder()
                .projectId(projectId)
                .name(request.getName())
                .description(request.getDescription())
                .createdBy(currentUser.getId())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .nodes(Collections.emptyList())
                .edges(Collections.emptyList())
                .build();

        Workflow saved = workflowRepository.save(workflow);

        return WorkflowSummaryResponse.builder()
                .id(saved.getId())
                .projectId(saved.getProjectId())
                .name(saved.getName())
                .description(saved.getDescription())
                .nodesCount(0)
                .edgesCount(0)
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    public WorkflowDiagramResponse getWorkflow(String projectId, String workflowId) {
        User currentUser = getCurrentUser();
        getMembership(projectId, currentUser.getId());

        Workflow workflow = workflowRepository.findByIdAndProjectId(workflowId, projectId)
                .orElseThrow(() -> new RuntimeException("Workflow no encontrado"));

        return WorkflowDiagramResponse.builder()
                .workflowId(workflow.getId())
                .projectId(workflow.getProjectId())
                .name(workflow.getName())
                .description(workflow.getDescription())
                .nodes(workflow.getNodes() != null ? workflow.getNodes() : Collections.emptyList())
                .edges(workflow.getEdges() != null ? workflow.getEdges() : Collections.emptyList())
                .build();
    }

    public MessageResponse saveWorkflow(String projectId, String workflowId, WorkflowDiagramRequest request) {
        User currentUser = getCurrentUser();
        ProjectMember membership = getMembership(projectId, currentUser.getId());
        validateAdmin(membership);

        Workflow workflow = workflowRepository.findByIdAndProjectId(workflowId, projectId)
                .orElseThrow(() -> new RuntimeException("Workflow no encontrado"));

        workflow.setNodes(request.getNodes() != null ? request.getNodes() : Collections.emptyList());
        workflow.setEdges(request.getEdges() != null ? request.getEdges() : Collections.emptyList());
        workflow.setUpdatedAt(LocalDateTime.now());

        workflowRepository.save(workflow);

        return new MessageResponse("Workflow guardado correctamente");
    }

    public MessageResponse deleteWorkflow(String projectId, String workflowId) {
        User currentUser = getCurrentUser();
        ProjectMember membership = getMembership(projectId, currentUser.getId());
        validateAdmin(membership);

        Workflow workflow = workflowRepository.findByIdAndProjectId(workflowId, projectId)
                .orElseThrow(() -> new RuntimeException("Workflow no encontrado"));

        workflowRepository.delete(workflow);

        return new MessageResponse("Workflow eliminado correctamente");
    }
}