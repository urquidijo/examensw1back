package com.parcial1.service;

import com.parcial1.dto.*;
import com.parcial1.model.*;
import com.parcial1.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final WorkflowTaskRepository workflowTaskRepository;
    private final WorkflowRepository workflowRepository;
    private final TicketRepository ticketRepository;
    private final DepartmentRepository departmentRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final TramiteTemplateRepository tramiteTemplateRepository;
    private final TicketStepHistoryRepository ticketStepHistoryRepository;

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

    private WorkflowTaskResponse mapTask(WorkflowTask task) {
        return WorkflowTaskResponse.builder()
                .id(task.getId())
                .projectId(task.getProjectId())
                .ticketId(task.getTicketId())
                .workflowId(task.getWorkflowId())
                .nodeId(task.getNodeId())
                .nodeLabel(task.getNodeLabel())
                .departmentId(task.getDepartmentId())
                .departmentName(task.getDepartmentName())
                .assignedUserId(task.getAssignedUserId())
                .assignedUserName(task.getAssignedUserName())
                .requiresTramite(task.isRequiresTramite())
                .tramiteTemplateId(task.getTramiteTemplateId())
                .tramiteTemplateName(task.getTramiteTemplateName())
                .status(task.getStatus())
                .submittedTramiteData(task.getSubmittedTramiteData())
                .createdAt(task.getCreatedAt())
                .startedAt(task.getStartedAt())
                .completedAt(task.getCompletedAt())
                .build();
    }

    private TicketStepHistoryResponse mapHistory(TicketStepHistory item) {
        return TicketStepHistoryResponse.builder()
                .id(item.getId())
                .projectId(item.getProjectId())
                .ticketId(item.getTicketId())
                .workflowId(item.getWorkflowId())
                .nodeId(item.getNodeId())
                .nodeLabel(item.getNodeLabel())
                .departmentId(item.getDepartmentId())
                .departmentName(item.getDepartmentName())
                .assignedUserId(item.getAssignedUserId())
                .assignedUserName(item.getAssignedUserName())
                .requiresTramite(item.isRequiresTramite())
                .tramiteTemplateId(item.getTramiteTemplateId())
                .tramiteTemplateName(item.getTramiteTemplateName())
                .submittedTramiteData(item.getSubmittedTramiteData())
                .startedAt(item.getStartedAt())
                .completedAt(item.getCompletedAt())
                .build();
    }

    public List<DepartmentTaskBoardResponse> getTaskBoardDepartments(String projectId) {
        User currentUser = getCurrentUser();
        ProjectMember membership = getMembership(projectId, currentUser.getId());

        List<Department> departments = departmentRepository.findByProjectIdOrderByCreatedAtDesc(projectId);

        if (membership.getRole() != ProjectRole.ADMINISTRADOR) {
            departments = departments.stream()
                    .filter(dep -> dep.getAssignedUserIds() != null && dep.getAssignedUserIds().contains(currentUser.getId()))
                    .toList();
        }

        return departments.stream()
                .map(dep -> {
                    long activeCount = workflowTaskRepository
                            .findByDepartmentIdOrderByCreatedAtDesc(dep.getId())
                            .stream()
                            .filter(task ->
                                    task.getProjectId().equals(projectId) &&
                                    (task.getStatus() == TaskStatus.PENDING || task.getStatus() == TaskStatus.IN_PROGRESS)
                            )
                            .count();

                    long completedCount = ticketStepHistoryRepository
                            .findByProjectIdAndDepartmentIdOrderByCompletedAtDesc(projectId, dep.getId())
                            .size();

                    boolean assignedToMe = dep.getAssignedUserIds() != null && dep.getAssignedUserIds().contains(currentUser.getId());

                    return DepartmentTaskBoardResponse.builder()
                            .departmentId(dep.getId())
                            .departmentName(dep.getName())
                            .activeTasksCount(activeCount)
                            .completedTasksCount(completedCount)
                            .assignedToMe(assignedToMe)
                            .build();
                })
                .toList();
    }

    public List<WorkflowTaskResponse> getMyDepartmentTasks(String projectId, String departmentId) {
        User currentUser = getCurrentUser();
        ProjectMember membership = getMembership(projectId, currentUser.getId());

        Department department = departmentRepository.findByIdAndProjectId(departmentId, projectId)
                .orElseThrow(() -> new RuntimeException("Departamento no encontrado"));

        if (membership.getRole() != ProjectRole.ADMINISTRADOR &&
                (department.getAssignedUserIds() == null || !department.getAssignedUserIds().contains(currentUser.getId()))) {
            throw new RuntimeException("No perteneces a este departamento");
        }

        if (membership.getRole() == ProjectRole.ADMINISTRADOR) {
            return workflowTaskRepository.findByDepartmentIdOrderByCreatedAtDesc(departmentId)
                    .stream()
                    .filter(task -> task.getProjectId().equals(projectId))
                    .filter(task -> task.getStatus() == TaskStatus.PENDING || task.getStatus() == TaskStatus.IN_PROGRESS)
                    .map(this::mapTask)
                    .toList();
        }

        return workflowTaskRepository
                .findByProjectIdAndAssignedUserIdAndDepartmentIdAndStatusInOrderByCreatedAtDesc(
                        projectId,
                        currentUser.getId(),
                        departmentId,
                        List.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS)
                )
                .stream()
                .map(this::mapTask)
                .toList();
    }

    public WorkflowTaskResponse getTaskDetail(String projectId, String taskId) {
        User currentUser = getCurrentUser();
        ProjectMember membership = getMembership(projectId, currentUser.getId());

        WorkflowTask task = workflowTaskRepository.findByIdAndProjectId(taskId, projectId)
                .orElseThrow(() -> new RuntimeException("Tarea no encontrada"));

        if (membership.getRole() != ProjectRole.ADMINISTRADOR) {
            if (task.getAssignedUserId() == null || !task.getAssignedUserId().equals(currentUser.getId())) {
                throw new RuntimeException("No tienes acceso a esta tarea");
            }
        }

        return mapTask(task);
    }

    public List<TicketStepHistoryResponse> getDepartmentCompletedHistory(String projectId, String departmentId) {
        User currentUser = getCurrentUser();
        ProjectMember membership = getMembership(projectId, currentUser.getId());

        Department department = departmentRepository.findByIdAndProjectId(departmentId, projectId)
                .orElseThrow(() -> new RuntimeException("Departamento no encontrado"));

        boolean isAssigned = department.getAssignedUserIds() != null && department.getAssignedUserIds().contains(currentUser.getId());

        if (membership.getRole() != ProjectRole.ADMINISTRADOR && !isAssigned) {
            throw new RuntimeException("No tienes acceso a este departamento");
        }

        return ticketStepHistoryRepository
                .findByProjectIdAndDepartmentIdOrderByCompletedAtDesc(projectId, departmentId)
                .stream()
                .map(this::mapHistory)
                .toList();
    }

    public WorkflowTaskResponse completeTask(String projectId, String taskId, CompleteTaskRequest request) {
        User currentUser = getCurrentUser();
        getMembership(projectId, currentUser.getId());

        WorkflowTask task = workflowTaskRepository.findByIdAndProjectId(taskId, projectId)
                .orElseThrow(() -> new RuntimeException("Tarea no encontrada"));

        if (task.getAssignedUserId() == null || !task.getAssignedUserId().equals(currentUser.getId())) {
            throw new RuntimeException("No puedes completar una tarea que no está asignada a ti");
        }

        if (task.getStatus() == TaskStatus.DONE) {
            throw new RuntimeException("La tarea ya fue completada");
        }

        if (task.isRequiresTramite() && (request == null || request.getTramiteData() == null || request.getTramiteData().isEmpty())) {
            throw new RuntimeException("Debes completar el trámite antes de finalizar");
        }

        Ticket ticket = ticketRepository.findByIdAndProjectId(task.getTicketId(), projectId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        Workflow workflow = workflowRepository.findByIdAndProjectId(task.getWorkflowId(), projectId)
                .orElseThrow(() -> new RuntimeException("Workflow no encontrado"));

        LocalDateTime now = LocalDateTime.now();

        task.setStatus(TaskStatus.DONE);
        task.setStartedAt(task.getStartedAt() != null ? task.getStartedAt() : task.getCreatedAt());
        task.setCompletedAt(now);
        task.setSubmittedTramiteData(request != null ? request.getTramiteData() : null);
        workflowTaskRepository.save(task);

        TicketStepHistory history = TicketStepHistory.builder()
                .projectId(projectId)
                .ticketId(ticket.getId())
                .workflowId(task.getWorkflowId())
                .nodeId(task.getNodeId())
                .nodeLabel(task.getNodeLabel())
                .departmentId(task.getDepartmentId())
                .departmentName(task.getDepartmentName())
                .assignedUserId(task.getAssignedUserId())
                .assignedUserName(task.getAssignedUserName())
                .requiresTramite(task.isRequiresTramite())
                .tramiteTemplateId(task.getTramiteTemplateId())
                .tramiteTemplateName(task.getTramiteTemplateName())
                .submittedTramiteData(task.getSubmittedTramiteData())
                .startedAt(task.getStartedAt())
                .completedAt(now)
                .build();

        ticketStepHistoryRepository.save(history);

        List<Map<String, Object>> nodes = workflow.getNodes() != null ? workflow.getNodes() : Collections.emptyList();
        List<Map<String, Object>> edges = workflow.getEdges() != null ? workflow.getEdges() : Collections.emptyList();

        Map<String, Object> nextNode = findNextOperationalNode(task.getNodeId(), nodes, edges);

        if (nextNode == null) {
            ticket.setStatus(TicketStatus.COMPLETED);
            ticket.setUpdatedAt(now);
            ticketRepository.save(ticket);
            return mapTask(task);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> nextNodeData = (Map<String, Object>) nextNode.get("data");

        String nextDepartmentId = nextNodeData != null ? String.valueOf(nextNodeData.getOrDefault("departmentId", "")) : "";
        String nextDepartmentName = nextNodeData != null ? String.valueOf(nextNodeData.getOrDefault("departmentName", "")) : "";
        String nextNodeId = String.valueOf(nextNode.getOrDefault("id", ""));
        String nextNodeLabel = nextNodeData != null
                ? String.valueOf(nextNodeData.getOrDefault("label", nextNode.getOrDefault("label", "Nodo")))
                : String.valueOf(nextNode.getOrDefault("label", "Nodo"));

        if (nextDepartmentId == null || nextDepartmentId.isBlank()) {
            throw new RuntimeException("El siguiente nodo no tiene departamento asignado");
        }

        Department nextDepartment = departmentRepository.findByIdAndProjectId(nextDepartmentId, projectId)
                .orElseThrow(() -> new RuntimeException("El siguiente departamento no existe"));

        String nextAssignedUserId = null;
        String nextAssignedUserName = null;

        if (nextDepartment.getAssignedUserIds() != null && !nextDepartment.getAssignedUserIds().isEmpty()) {
            nextAssignedUserId = nextDepartment.getAssignedUserIds().get(0);
            nextAssignedUserName = userRepository.findById(nextAssignedUserId)
                    .map(User::getName)
                    .orElse("Funcionario");
        }

        String nextTramiteName = null;
        if (nextDepartment.isRequiresTramite() && nextDepartment.getTramiteTemplateId() != null) {
            nextTramiteName = tramiteTemplateRepository.findById(nextDepartment.getTramiteTemplateId())
                    .map(TramiteTemplate::getName)
                    .orElse(null);
        }

        WorkflowTask nextTask = WorkflowTask.builder()
                .projectId(projectId)
                .ticketId(ticket.getId())
                .workflowId(workflow.getId())
                .nodeId(nextNodeId)
                .nodeLabel(nextNodeLabel)
                .departmentId(nextDepartment.getId())
                .departmentName(nextDepartmentName != null && !nextDepartmentName.isBlank() ? nextDepartmentName : nextDepartment.getName())
                .assignedUserId(nextAssignedUserId)
                .assignedUserName(nextAssignedUserName)
                .requiresTramite(nextDepartment.isRequiresTramite())
                .tramiteTemplateId(nextDepartment.getTramiteTemplateId())
                .tramiteTemplateName(nextTramiteName)
                .status(TaskStatus.PENDING)
                .createdAt(now)
                .build();

        workflowTaskRepository.save(nextTask);

        ticket.setStatus(TicketStatus.IN_PROGRESS);
        ticket.setCurrentNodeId(nextNodeId);
        ticket.setCurrentDepartmentId(nextDepartment.getId());
        ticket.setCurrentDepartmentName(nextDepartment.getName());
        ticket.setUpdatedAt(now);
        ticketRepository.save(ticket);

        return mapTask(task);
    }

    private Map<String, Object> findNextOperationalNode(
            String currentNodeId,
            List<Map<String, Object>> nodes,
            List<Map<String, Object>> edges
    ) {
        Map<String, Map<String, Object>> nodeMap = nodes.stream()
                .collect(Collectors.toMap(node -> String.valueOf(node.get("id")), node -> node, (a, b) -> a));

        String nextNodeId = findDirectTargetNodeId(currentNodeId, edges);
        Set<String> visited = new HashSet<>();

        while (nextNodeId != null && !visited.contains(nextNodeId)) {
            visited.add(nextNodeId);

            Map<String, Object> node = nodeMap.get(nextNodeId);
            if (node == null) {
                return null;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) node.get("data");
            String nodeType = data != null ? String.valueOf(data.getOrDefault("nodeType", "")) : "";

            if ("task".equals(nodeType) || "decision".equals(nodeType)) {
                return node;
            }

            if ("end".equals(nodeType)) {
                return null;
            }

            nextNodeId = findDirectTargetNodeId(nextNodeId, edges);
        }

        return null;
    }

    private String findDirectTargetNodeId(String sourceNodeId, List<Map<String, Object>> edges) {
        for (Map<String, Object> edge : edges) {
            Object sourceObj = edge.get("source");
            String sourceCellId = extractCellId(sourceObj);

            if (!sourceNodeId.equals(sourceCellId)) {
                continue;
            }

            return extractCellId(edge.get("target"));
        }

        return null;
    }

    private String extractCellId(Object endpoint) {
        if (endpoint instanceof String str) {
            return str;
        }

        if (endpoint instanceof Map<?, ?> map) {
            Object cell = map.get("cell");
            return cell != null ? String.valueOf(cell) : null;
        }

        return null;
    }
}