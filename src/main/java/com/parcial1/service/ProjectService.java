package com.parcial1.service;

import com.parcial1.dto.CreateProjectRequest;
import com.parcial1.dto.InviteUserRequest;
import com.parcial1.dto.ProjectSummaryResponse;
import com.parcial1.model.Project;
import com.parcial1.model.ProjectMember;
import com.parcial1.model.ProjectRole;
import com.parcial1.model.User;
import com.parcial1.repository.ProjectMemberRepository;
import com.parcial1.repository.ProjectRepository;
import com.parcial1.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
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

    public Project createProject(CreateProjectRequest request) {
        User currentUser = getCurrentUser();

        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .createdBy(currentUser.getId())
                .createdAt(LocalDateTime.now())
                .build();

        Project savedProject = projectRepository.save(project);

        ProjectMember member = ProjectMember.builder()
                .projectId(savedProject.getId())
                .userId(currentUser.getId())
                .role(ProjectRole.ADMINISTRADOR)
                .joinedAt(LocalDateTime.now())
                .build();

        projectMemberRepository.save(member);

        return savedProject;
    }

    public List<ProjectSummaryResponse> getMyProjects() {
        User currentUser = getCurrentUser();

        List<ProjectMember> memberships = projectMemberRepository.findByUserId(currentUser.getId());
        List<ProjectSummaryResponse> response = new ArrayList<>();

        for (ProjectMember membership : memberships) {
            Project project = projectRepository.findById(membership.getProjectId())
                    .orElse(null);

            if (project != null) {
                response.add(
                        ProjectSummaryResponse.builder()
                                .projectId(project.getId())
                                .name(project.getName())
                                .description(project.getDescription())
                                .role(membership.getRole())
                                .build()
                );
            }
        }

        return response;
    }

    public List<ProjectSummaryResponse> getOwnedProjects() {
        User currentUser = getCurrentUser();

        List<ProjectMember> memberships = projectMemberRepository.findByUserId(currentUser.getId());
        List<ProjectSummaryResponse> response = new ArrayList<>();

        for (ProjectMember membership : memberships) {
            if (membership.getRole() != ProjectRole.ADMINISTRADOR) {
                continue;
            }

            Project project = projectRepository.findById(membership.getProjectId())
                    .orElse(null);

            if (project != null) {
                response.add(
                        ProjectSummaryResponse.builder()
                                .projectId(project.getId())
                                .name(project.getName())
                                .description(project.getDescription())
                                .role(membership.getRole())
                                .build()
                );
            }
        }

        return response;
    }

    public List<ProjectSummaryResponse> getSharedProjects() {
        User currentUser = getCurrentUser();

        List<ProjectMember> memberships = projectMemberRepository.findByUserId(currentUser.getId());
        List<ProjectSummaryResponse> response = new ArrayList<>();

        for (ProjectMember membership : memberships) {
            if (membership.getRole() != ProjectRole.FUNCIONARIO) {
                continue;
            }

            Project project = projectRepository.findById(membership.getProjectId())
                    .orElse(null);

            if (project != null) {
                response.add(
                        ProjectSummaryResponse.builder()
                                .projectId(project.getId())
                                .name(project.getName())
                                .description(project.getDescription())
                                .role(membership.getRole())
                                .build()
                );
            }
        }

        return response;
    }

    public void inviteUser(String projectId, InviteUserRequest request) {
        User currentUser = getCurrentUser();

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));

        ProjectMember currentMembership = projectMemberRepository
                .findByProjectIdAndUserId(projectId, currentUser.getId())
                .orElseThrow(() -> new RuntimeException("No perteneces a este proyecto"));

        if (currentMembership.getRole() != ProjectRole.ADMINISTRADOR) {
            throw new RuntimeException("Solo el administrador puede invitar usuarios");
        }

        User invitedUser = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("El usuario invitado no existe"));

        boolean alreadyExists = projectMemberRepository
                .findByProjectIdAndUserId(projectId, invitedUser.getId())
                .isPresent();

        if (alreadyExists) {
            throw new RuntimeException("El usuario ya pertenece a este proyecto");
        }

        ProjectMember newMember = ProjectMember.builder()
                .projectId(project.getId())
                .userId(invitedUser.getId())
                .role(ProjectRole.FUNCIONARIO)
                .joinedAt(LocalDateTime.now())
                .build();

        projectMemberRepository.save(newMember);
    }

    public Project getProjectById(String projectId) {
        User currentUser = getCurrentUser();

        projectMemberRepository.findByProjectIdAndUserId(projectId, currentUser.getId())
                .orElseThrow(() -> new RuntimeException("No tienes acceso a este proyecto"));

        return projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));
    }

    public void deleteProject(String projectId) {
    User currentUser = getCurrentUser();

    Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));

    ProjectMember membership = projectMemberRepository
            .findByProjectIdAndUserId(projectId, currentUser.getId())
            .orElseThrow(() -> new RuntimeException("No perteneces a este proyecto"));

    if (membership.getRole() != ProjectRole.ADMINISTRADOR) {
        throw new RuntimeException("Solo el administrador puede eliminar el proyecto");
    }

    projectMemberRepository.deleteByProjectId(projectId);
    projectRepository.delete(project);
}
}