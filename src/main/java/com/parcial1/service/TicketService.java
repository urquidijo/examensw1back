package com.parcial1.service;

import com.parcial1.dto.CreateTicketRequest;
import com.parcial1.dto.TicketResponse;
import com.parcial1.model.*;
import com.parcial1.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final WorkflowTaskRepository workflowTaskRepository;
    private final WorkflowRepository workflowRepository;
    private final DepartmentRepository departmentRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final TramiteTemplateRepository tramiteTemplateRepository;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Usuario no autenticado");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado"));
    }

    private ProjectMember getMembership(String projectId, String userId) {
        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new RuntimeException("No tienes acceso a este proyecto"));
    }

    private TicketResponse mapTicket(Ticket ticket) {
        return TicketResponse.builder()
                .id(ticket.getId())
                .projectId(ticket.getProjectId())
                .workflowId(ticket.getWorkflowId())
                .workflowName(ticket.getWorkflowName())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .clientName(ticket.getClientName())
                .clientPhone(ticket.getClientPhone())
                .clientEmail(ticket.getClientEmail())
                .clientReference(ticket.getClientReference())
                .status(ticket.getStatus())
                .currentDepartmentId(ticket.getCurrentDepartmentId())
                .currentDepartmentName(ticket.getCurrentDepartmentName())
                .currentNodeId(ticket.getCurrentNodeId())
                .createdBy(ticket.getCreatedBy())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .metadata(ticket.getMetadata())
                .build();
    }

    public List<TicketResponse> getTickets(String projectId) {
        User currentUser = getCurrentUser();
        getMembership(projectId, currentUser.getId());

        return ticketRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(this::mapTicket)
                .toList();
    }

    public TicketResponse createTicket(String projectId, CreateTicketRequest request) {
        User currentUser = getCurrentUser();
        getMembership(projectId, currentUser.getId());

        if (request.getWorkflowId() == null || request.getWorkflowId().trim().isBlank()) {
            throw new RuntimeException("Debes seleccionar un workflow");
        }

        Workflow workflow = workflowRepository.findByIdAndProjectId(request.getWorkflowId(), projectId)
                .orElseThrow(() -> new RuntimeException("Workflow no encontrado"));

        if (workflow.getStatus() != WorkflowStatus.PUBLISHED) {
            throw new RuntimeException("Solo puedes crear tickets con workflows en producción");
        }

        List<Map<String, Object>> nodes = workflow.getNodes() != null ? workflow.getNodes() : Collections.emptyList();

        Map<String, Object> firstOperationalNode = nodes.stream()
                .filter(node -> {
                    Object dataObj = node.get("data");
                    if (!(dataObj instanceof Map<?, ?> data)) return false;

                    Object nodeType = data.get("nodeType");
                    return "task".equals(String.valueOf(nodeType)) || "decision".equals(String.valueOf(nodeType));
                })
                .findFirst()
                .orElseThrow(() -> new RuntimeException("El workflow no tiene un primer nodo operativo"));

        @SuppressWarnings("unchecked")
        Map<String, Object> nodeData = (Map<String, Object>) firstOperationalNode.get("data");

        String departmentId = nodeData != null ? String.valueOf(nodeData.getOrDefault("departmentId", "")) : "";
        String departmentName = nodeData != null ? String.valueOf(nodeData.getOrDefault("departmentName", "")) : "";
        String nodeId = String.valueOf(firstOperationalNode.getOrDefault("id", ""));
        String nodeLabel = nodeData != null
                ? String.valueOf(nodeData.getOrDefault("label", firstOperationalNode.getOrDefault("label", "Nodo")))
                : String.valueOf(firstOperationalNode.getOrDefault("label", "Nodo"));

        if (departmentId == null || departmentId.isBlank()) {
            throw new RuntimeException("El primer nodo operativo no tiene departamento asignado");
        }

        Department department = departmentRepository.findByIdAndProjectId(departmentId, projectId)
                .orElseThrow(() -> new RuntimeException("El departamento del primer nodo no existe"));

        String assignedUserId = null;
        String assignedUserName = null;

        if (department.getAssignedUserIds() != null && !department.getAssignedUserIds().isEmpty()) {
            assignedUserId = department.getAssignedUserIds().get(0);

            assignedUserName = userRepository.findById(assignedUserId)
                    .map(User::getName)
                    .orElse("Funcionario");
        }

        String tramiteTemplateName = null;
        if (department.isRequiresTramite() && department.getTramiteTemplateId() != null) {
            tramiteTemplateName = tramiteTemplateRepository.findById(department.getTramiteTemplateId())
                    .map(TramiteTemplate::getName)
                    .orElse(null);
        }

        LocalDateTime now = LocalDateTime.now();

        Ticket ticket = Ticket.builder()
                .projectId(projectId)
                .workflowId(workflow.getId())
                .workflowName(workflow.getName())
                .title(request.getTitle())
                .description(request.getDescription())
                .clientName(request.getClientName())
                .clientPhone(request.getClientPhone())
                .clientEmail(request.getClientEmail())
                .clientReference(request.getClientReference())
                .status(TicketStatus.OPEN)
                .currentDepartmentId(department.getId())
                .currentDepartmentName(departmentName != null && !departmentName.isBlank() ? departmentName : department.getName())
                .currentNodeId(nodeId)
                .createdBy(currentUser.getId())
                .createdAt(now)
                .updatedAt(now)
                .metadata(request.getMetadata())
                .build();

        Ticket savedTicket = ticketRepository.save(ticket);

        WorkflowTask task = WorkflowTask.builder()
                .projectId(projectId)
                .ticketId(savedTicket.getId())
                .workflowId(workflow.getId())
                .nodeId(nodeId)
                .nodeLabel(nodeLabel)
                .departmentId(department.getId())
                .departmentName(department.getName())
                .assignedUserId(assignedUserId)
                .assignedUserName(assignedUserName)
                .requiresTramite(department.isRequiresTramite())
                .tramiteTemplateId(department.getTramiteTemplateId())
                .tramiteTemplateName(tramiteTemplateName)
                .status(TaskStatus.PENDING)
                .createdAt(now)
                .build();

        workflowTaskRepository.save(task);

        return mapTicket(savedTicket);
    }
}