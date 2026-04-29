package com.parcial1.service;

import com.parcial1.dto.CreateTicketRequest;
import com.parcial1.dto.TicketFileDownloadResponse;
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
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final WorkflowRepository workflowRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final TaskService taskService;
    private final S3StorageService s3StorageService;

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
                .currentDepartmentEnteredAt(ticket.getCurrentDepartmentEnteredAt())
                .currentNodeId(ticket.getCurrentNodeId())
                .createdBy(ticket.getCreatedBy())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .metadata(ticket.getMetadata())
                .uploadedFiles(ticket.getUploadedFiles())
                .build();
    }

    public TicketFileDownloadResponse downloadTicketFile(String projectId, String ticketId, String key) {
        User currentUser = getCurrentUser();
        getMembership(projectId, currentUser.getId());

        Ticket ticket = ticketRepository.findByIdAndProjectId(ticketId, projectId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        StoredFileInfo fileInfo = ticket.getUploadedFiles() == null
                ? null
                : ticket.getUploadedFiles().stream()
                        .filter(file -> key.equals(file.getKey()))
                        .findFirst()
                        .orElse(null);

        if (fileInfo == null) {
            throw new RuntimeException("Archivo del ticket no encontrado");
        }

        byte[] content = s3StorageService.download(fileInfo.getKey());

        return TicketFileDownloadResponse.builder()
                .content(content)
                .originalName(fileInfo.getOriginalName())
                .contentType(fileInfo.getContentType())
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

    public TicketResponse createTicket(String projectId, CreateTicketRequest request, List<MultipartFile> files) {
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
                .currentDepartmentEnteredAt(null)
                .currentNodeId(null)
                .createdBy(currentUser.getId())
                .createdAt(now)
                .updatedAt(now)
                .metadata(request.getMetadata())
                .uploadedFiles(new ArrayList<>())
                .build();

        Ticket savedTicket = ticketRepository.save(ticket);

        List<StoredFileInfo> uploadedFiles = new ArrayList<>();

        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    continue;
                }

                try {
                    StoredFileInfo storedFile = s3StorageService.uploadTicketAttachment(
                            savedTicket.getId(),
                            file,
                            currentUser.getId());
                    uploadedFiles.add(storedFile);
                } catch (Exception e) {
                    log.error("ERROR SUBIENDO ARCHIVO DEL TICKET A S3", e);
                    throw new RuntimeException("No se pudo subir uno de los archivos del ticket: " + e.getMessage(), e);
                }
            }
        }

        savedTicket.setUploadedFiles(uploadedFiles);
        savedTicket.setUpdatedAt(LocalDateTime.now());
        savedTicket = ticketRepository.save(savedTicket);

        taskService.startWorkflowForTicket(savedTicket, workflow);

        Ticket updatedTicket = ticketRepository.findById(savedTicket.getId())
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado después de iniciar el workflow"));

        return mapTicket(updatedTicket);
    }
}