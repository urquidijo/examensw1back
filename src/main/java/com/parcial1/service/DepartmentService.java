package com.parcial1.service;

import com.parcial1.dto.CreateDepartmentRequest;
import com.parcial1.dto.DepartmentAssignedUserResponse;
import com.parcial1.dto.DepartmentResponse;
import com.parcial1.dto.MessageResponse;
import com.parcial1.dto.UpdateDepartmentRequest;
import com.parcial1.model.Department;
import com.parcial1.model.ProjectMember;
import com.parcial1.model.ProjectRole;
import com.parcial1.model.TramiteTemplate;
import com.parcial1.model.User;
import com.parcial1.repository.DepartmentRepository;
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
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TramiteTemplateRepository tramiteTemplateRepository;
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
            throw new RuntimeException("Solo el administrador puede gestionar departamentos");
        }
    }

    private void validateAssignedUsers(String projectId, List<String> assignedUserIds) {
        if (assignedUserIds == null) {
            return;
        }

        for (String userId : assignedUserIds) {
            boolean exists = projectMemberRepository.existsByProjectIdAndUserId(projectId, userId);
            if (!exists) {
                throw new RuntimeException("Uno de los usuarios asignados no pertenece al proyecto");
            }
        }
    }

    private TramiteTemplate validateAndGetTramite(String projectId, boolean requiresTramite, String tramiteTemplateId) {
        if (!requiresTramite) {
            return null;
        }

        if (tramiteTemplateId == null || tramiteTemplateId.trim().isBlank()) {
            throw new RuntimeException("Debes seleccionar un trámite para este departamento");
        }

        return tramiteTemplateRepository.findByIdAndProjectId(tramiteTemplateId, projectId)
                .orElseThrow(() -> new RuntimeException("El trámite seleccionado no existe en este proyecto"));
    }

    private DepartmentResponse mapToResponse(Department department) {
        List<String> assignedIds = department.getAssignedUserIds() != null
                ? department.getAssignedUserIds()
                : Collections.emptyList();

        List<DepartmentAssignedUserResponse> assignedUsers = assignedIds.stream()
                .map(userId -> userRepository.findById(userId)
                        .map(user -> DepartmentAssignedUserResponse.builder()
                                .userId(user.getId())
                                .name(user.getName())
                                .email(user.getEmail())
                                .build())
                        .orElse(null))
                .filter(item -> item != null)
                .toList();

        String tramiteName = null;
        if (department.isRequiresTramite() && department.getTramiteTemplateId() != null) {
            tramiteName = tramiteTemplateRepository.findById(department.getTramiteTemplateId())
                    .map(TramiteTemplate::getName)
                    .orElse(null);
        }

        return DepartmentResponse.builder()
                .id(department.getId())
                .projectId(department.getProjectId())
                .name(department.getName())
                .description(department.getDescription())
                .assignedUserIds(assignedIds)
                .assignedUsers(assignedUsers)
                .requiresTramite(department.isRequiresTramite())
                .tramiteTemplateId(department.getTramiteTemplateId())
                .tramiteName(tramiteName)
                .createdBy(department.getCreatedBy())
                .createdAt(department.getCreatedAt())
                .updatedAt(department.getUpdatedAt())
                .build();
    }

    public List<DepartmentResponse> getDepartments(String projectId) {
        User currentUser = getCurrentUser();
        getMembership(projectId, currentUser.getId());

        return departmentRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public DepartmentResponse getDepartmentById(String projectId, String departmentId) {
        User currentUser = getCurrentUser();
        getMembership(projectId, currentUser.getId());

        Department department = departmentRepository.findByIdAndProjectId(departmentId, projectId)
                .orElseThrow(() -> new RuntimeException("Departamento no encontrado"));

        return mapToResponse(department);
    }

    public DepartmentResponse createDepartment(String projectId, CreateDepartmentRequest request) {
        User currentUser = getCurrentUser();

        projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));

        ProjectMember membership = getMembership(projectId, currentUser.getId());
        validateAdmin(membership);

        String cleanName = request.getName().trim();

        if (departmentRepository.existsByProjectIdAndNameIgnoreCase(projectId, cleanName)) {
            throw new RuntimeException("Ya existe un departamento con ese nombre en este proyecto");
        }

        List<String> assignedUserIds = request.getAssignedUserIds() != null
                ? request.getAssignedUserIds()
                : Collections.emptyList();

        validateAssignedUsers(projectId, assignedUserIds);

        boolean requiresTramite = request.getRequiresTramite() != null && request.getRequiresTramite();
        TramiteTemplate tramite = validateAndGetTramite(projectId, requiresTramite, request.getTramiteTemplateId());

        Department department = Department.builder()
                .projectId(projectId)
                .name(cleanName)
                .description(request.getDescription() != null ? request.getDescription().trim() : "")
                .assignedUserIds(assignedUserIds)
                .requiresTramite(requiresTramite)
                .tramiteTemplateId(tramite != null ? tramite.getId() : null)
                .createdBy(currentUser.getId())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Department saved = departmentRepository.save(department);

        return mapToResponse(saved);
    }

    public DepartmentResponse updateDepartment(String projectId, String departmentId, UpdateDepartmentRequest request) {
        User currentUser = getCurrentUser();
        ProjectMember membership = getMembership(projectId, currentUser.getId());
        validateAdmin(membership);

        Department department = departmentRepository.findByIdAndProjectId(departmentId, projectId)
                .orElseThrow(() -> new RuntimeException("Departamento no encontrado"));

        if (request.getName() != null && !request.getName().trim().isBlank()) {
            String cleanName = request.getName().trim();

            boolean duplicateName =
                    departmentRepository.existsByProjectIdAndNameIgnoreCase(projectId, cleanName)
                    && !department.getName().equalsIgnoreCase(cleanName);

            if (duplicateName) {
                throw new RuntimeException("Ya existe otro departamento con ese nombre en este proyecto");
            }

            department.setName(cleanName);
        }

        if (request.getDescription() != null) {
            department.setDescription(request.getDescription().trim());
        }

        if (request.getAssignedUserIds() != null) {
            validateAssignedUsers(projectId, request.getAssignedUserIds());
            department.setAssignedUserIds(request.getAssignedUserIds());
        }

        boolean requiresTramite = request.getRequiresTramite() != null
                ? request.getRequiresTramite()
                : department.isRequiresTramite();

        String tramiteTemplateId = request.getTramiteTemplateId() != null
                ? request.getTramiteTemplateId()
                : department.getTramiteTemplateId();

        TramiteTemplate tramite = validateAndGetTramite(projectId, requiresTramite, tramiteTemplateId);

        department.setRequiresTramite(requiresTramite);
        department.setTramiteTemplateId(tramite != null ? tramite.getId() : null);
        department.setUpdatedAt(LocalDateTime.now());

        Department saved = departmentRepository.save(department);

        return mapToResponse(saved);
    }

    public MessageResponse deleteDepartment(String projectId, String departmentId) {
        User currentUser = getCurrentUser();
        ProjectMember membership = getMembership(projectId, currentUser.getId());
        validateAdmin(membership);

        Department department = departmentRepository.findByIdAndProjectId(departmentId, projectId)
                .orElseThrow(() -> new RuntimeException("Departamento no encontrado"));

        departmentRepository.delete(department);

        return new MessageResponse("Departamento eliminado correctamente");
    }
}