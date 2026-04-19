package com.parcial1.service;

import com.parcial1.dto.CreateTicketRequest;
import com.parcial1.dto.TicketResponse;
import com.parcial1.model.*;
import com.parcial1.repository.ProjectMemberRepository;
import com.parcial1.repository.TicketRepository;
import com.parcial1.repository.UserRepository;
import com.parcial1.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final WorkflowRepository workflowRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final TaskService taskService;

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
                .currentDepartmentId(null)
                .currentDepartmentName(null)
                .currentNodeId(null)
                .createdBy(currentUser.getId())
                .createdAt(now)
                .updatedAt(now)
                .metadata(request.getMetadata())
                .build();

        Ticket savedTicket = ticketRepository.save(ticket);

        taskService.startWorkflowForTicket(savedTicket, workflow);

        Ticket updatedTicket = ticketRepository.findById(savedTicket.getId())
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado después de iniciar el workflow"));

        return mapTicket(updatedTicket);
    }
}