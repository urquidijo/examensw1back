package com.parcial1.service;

import com.parcial1.dto.CreateTramiteRequest;
import com.parcial1.dto.MessageResponse;
import com.parcial1.dto.TramiteResponse;
import com.parcial1.dto.UpdateTramiteRequest;
import com.parcial1.model.ProjectMember;
import com.parcial1.model.ProjectRole;
import com.parcial1.model.TramiteTemplate;
import com.parcial1.model.User;
import com.parcial1.repository.ProjectMemberRepository;
import com.parcial1.repository.ProjectRepository;
import com.parcial1.repository.TramiteTemplateRepository;
import com.parcial1.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TramiteTemplateService {

    private final TramiteTemplateRepository tramiteTemplateRepository;
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
                .orElseThrow(() -> new RuntimeException("No tienes acceso a este proyecto"));
    }

    private void validateAdmin(ProjectMember membership) {
        if (membership.getRole() != ProjectRole.ADMINISTRADOR) {
            throw new RuntimeException("Solo el administrador puede gestionar trámites");
        }
    }

    private TramiteResponse mapToResponse(TramiteTemplate tramite) {
        return TramiteResponse.builder()
                .id(tramite.getId())
                .projectId(tramite.getProjectId())
                .name(tramite.getName())
                .description(tramite.getDescription())
                .active(tramite.isActive())
                .fieldsCount(tramite.getFields() != null ? tramite.getFields().size() : 0)
                .createdBy(tramite.getCreatedBy())
                .createdAt(tramite.getCreatedAt())
                .updatedAt(tramite.getUpdatedAt())
                .fields(tramite.getFields() != null ? tramite.getFields() : Collections.emptyList())
                .build();
    }

    public List<TramiteResponse> getTramites(String projectId) {
        User currentUser = getCurrentUser();
        getMembership(projectId, currentUser.getId());

        return tramiteTemplateRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public TramiteResponse getTramiteById(String projectId, String tramiteId) {
        User currentUser = getCurrentUser();
        getMembership(projectId, currentUser.getId());

        TramiteTemplate tramite = tramiteTemplateRepository.findByIdAndProjectId(tramiteId, projectId)
                .orElseThrow(() -> new RuntimeException("Trámite no encontrado"));

        return mapToResponse(tramite);
    }

    public TramiteResponse createTramite(String projectId, CreateTramiteRequest request) {
        User currentUser = getCurrentUser();

        projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));

        ProjectMember membership = getMembership(projectId, currentUser.getId());
        validateAdmin(membership);

        String cleanName = request.getName().trim();

        if (tramiteTemplateRepository.existsByProjectIdAndNameIgnoreCase(projectId, cleanName)) {
            throw new RuntimeException("Ya existe un trámite con ese nombre en este proyecto");
        }

        TramiteTemplate tramite = TramiteTemplate.builder()
                .projectId(projectId)
                .name(cleanName)
                .description(request.getDescription() != null ? request.getDescription().trim() : "")
                .active(request.getActive() != null ? request.getActive() : true)
                .createdBy(currentUser.getId())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .fields(request.getFields() != null ? request.getFields() : Collections.emptyList())
                .build();

        TramiteTemplate saved = tramiteTemplateRepository.save(tramite);

        return mapToResponse(saved);
    }

    public TramiteResponse updateTramite(String projectId, String tramiteId, UpdateTramiteRequest request) {
        User currentUser = getCurrentUser();
        ProjectMember membership = getMembership(projectId, currentUser.getId());
        validateAdmin(membership);

        TramiteTemplate tramite = tramiteTemplateRepository.findByIdAndProjectId(tramiteId, projectId)
                .orElseThrow(() -> new RuntimeException("Trámite no encontrado"));

        if (request.getName() != null && !request.getName().trim().isBlank()) {
            String cleanName = request.getName().trim();

            boolean duplicateName =
                    tramiteTemplateRepository.existsByProjectIdAndNameIgnoreCase(projectId, cleanName)
                    && !tramite.getName().equalsIgnoreCase(cleanName);

            if (duplicateName) {
                throw new RuntimeException("Ya existe otro trámite con ese nombre en este proyecto");
            }

            tramite.setName(cleanName);
        }

        if (request.getDescription() != null) {
            tramite.setDescription(request.getDescription().trim());
        }

        if (request.getActive() != null) {
            tramite.setActive(request.getActive());
        }

        if (request.getFields() != null) {
            tramite.setFields(request.getFields());
        }

        tramite.setUpdatedAt(LocalDateTime.now());

        TramiteTemplate saved = tramiteTemplateRepository.save(tramite);

        return mapToResponse(saved);
    }

    public MessageResponse deleteTramite(String projectId, String tramiteId) {
        User currentUser = getCurrentUser();
        ProjectMember membership = getMembership(projectId, currentUser.getId());
        validateAdmin(membership);

        TramiteTemplate tramite = tramiteTemplateRepository.findByIdAndProjectId(tramiteId, projectId)
                .orElseThrow(() -> new RuntimeException("Trámite no encontrado"));

        tramiteTemplateRepository.delete(tramite);

        return new MessageResponse("Trámite eliminado correctamente");
    }
}