package com.parcial1.service;

import com.parcial1.dto.*;
import com.parcial1.model.*;
import com.parcial1.repository.*;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
    private final ParallelJoinStateRepository parallelJoinStateRepository;
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

    private WorkflowTaskResponse mapTask(WorkflowTask task) {
        Ticket ticket = ticketRepository.findByIdAndProjectId(task.getTicketId(), task.getProjectId())
                .orElse(null);

        return WorkflowTaskResponse.builder()
                .id(task.getId())
                .projectId(task.getProjectId())
                .ticketId(task.getTicketId())
                .workflowId(task.getWorkflowId())
                .nodeId(task.getNodeId())
                .nodeLabel(task.getNodeLabel())
                .nodeType(task.getNodeType())
                .departmentId(task.getDepartmentId())
                .departmentName(task.getDepartmentName())
                .assignedUserId(task.getAssignedUserId())
                .assignedUserName(task.getAssignedUserName())
                .requiresTramite(task.isRequiresTramite())
                .tramiteTemplateId(task.getTramiteTemplateId())
                .tramiteTemplateName(task.getTramiteTemplateName())
                .decisionMode(task.getDecisionMode())
                .decisionQuestion(task.getDecisionQuestion())
                .decisionOptions(task.getDecisionOptions())
                .status(task.getStatus())
                .submittedTramiteData(task.getSubmittedTramiteData())
                .uploadedFiles(task.getUploadedFiles())
                .ticket(mapTicketInfo(ticket))
                .createdAt(task.getCreatedAt())
                .startedAt(task.getStartedAt())
                .completedAt(task.getCompletedAt())
                .build();
    }

    public TaskFileDownloadResponse downloadCompletedHistoryFile(String projectId, String historyId, String key) {
        User currentUser = getCurrentUser();
        getMembership(projectId, currentUser.getId());

        TicketStepHistory history = ticketStepHistoryRepository.findByIdAndProjectId(historyId, projectId)
                .orElseThrow(() -> new RuntimeException("Histórico de tarea no encontrado"));

        StoredFileInfo fileInfo = history.getUploadedFiles() == null
                ? null
                : history.getUploadedFiles().stream()
                        .filter(file -> key.equals(file.getKey()))
                        .findFirst()
                        .orElse(null);

        if (fileInfo == null) {
            throw new RuntimeException("Archivo del histórico no encontrado");
        }

        byte[] content = s3StorageService.download(fileInfo.getKey());

        return TaskFileDownloadResponse.builder()
                .content(content)
                .originalName(fileInfo.getOriginalName())
                .contentType(fileInfo.getContentType())
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
                .nodeType(item.getNodeType())
                .departmentId(item.getDepartmentId())
                .departmentName(item.getDepartmentName())
                .assignedUserId(item.getAssignedUserId())
                .assignedUserName(item.getAssignedUserName())
                .requiresTramite(item.isRequiresTramite())
                .uploadedFiles(item.getUploadedFiles())
                .tramiteTemplateId(item.getTramiteTemplateId())
                .tramiteTemplateName(item.getTramiteTemplateName())
                .decisionResult(item.getDecisionResult())
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
                    .filter(dep -> dep.getAssignedUserIds() != null
                            && dep.getAssignedUserIds().contains(currentUser.getId()))
                    .toList();
        }

        return departments.stream()
                .map(dep -> {
                    long activeCount = workflowTaskRepository
                            .findByDepartmentIdOrderByCreatedAtDesc(dep.getId())
                            .stream()
                            .filter(task -> task.getProjectId().equals(projectId) &&
                                    (task.getStatus() == TaskStatus.PENDING
                                            || task.getStatus() == TaskStatus.IN_PROGRESS))
                            .count();

                    long completedCount = ticketStepHistoryRepository
                            .findByProjectIdAndDepartmentIdOrderByCompletedAtDesc(projectId, dep.getId())
                            .size();

                    boolean assignedToMe = dep.getAssignedUserIds() != null
                            && dep.getAssignedUserIds().contains(currentUser.getId());

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
                (department.getAssignedUserIds() == null
                        || !department.getAssignedUserIds().contains(currentUser.getId()))) {
            throw new RuntimeException("No perteneces a este departamento");
        }

        if (membership.getRole() == ProjectRole.ADMINISTRADOR) {
            return workflowTaskRepository.findByDepartmentIdOrderByCreatedAtDesc(departmentId)
                    .stream()
                    .filter(task -> task.getProjectId().equals(projectId))
                    .filter(task -> task.getStatus() == TaskStatus.PENDING
                            || task.getStatus() == TaskStatus.IN_PROGRESS)
                    .map(this::mapTask)
                    .toList();
        }

        return workflowTaskRepository
                .findByProjectIdAndAssignedUserIdAndDepartmentIdAndStatusInOrderByCreatedAtDesc(
                        projectId,
                        currentUser.getId(),
                        departmentId,
                        List.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS))
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

        boolean isAssigned = department.getAssignedUserIds() != null
                && department.getAssignedUserIds().contains(currentUser.getId());

        if (membership.getRole() != ProjectRole.ADMINISTRADOR && !isAssigned) {
            throw new RuntimeException("No tienes acceso a este departamento");
        }

        return ticketStepHistoryRepository
                .findByProjectIdAndDepartmentIdOrderByCompletedAtDesc(projectId, departmentId)
                .stream()
                .map(this::mapHistory)
                .toList();
    }

    public WorkflowTaskResponse completeTask(
            String projectId,
            String taskId,
            CompleteTaskRequest request,
            List<MultipartFile> files) {
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

        if (task.isRequiresTramite()
                && (request == null || request.getTramiteData() == null || request.getTramiteData().isEmpty())) {
            throw new RuntimeException("Debes completar el trámite antes de finalizar");
        }

        if ("decision".equalsIgnoreCase(task.getNodeType())) {
            if (request == null || request.getDecisionResult() == null || request.getDecisionResult().isBlank()) {
                throw new RuntimeException("Debes seleccionar una opción de decisión");
            }
        }

        Ticket ticket = ticketRepository.findByIdAndProjectId(task.getTicketId(), projectId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        Workflow workflow = workflowRepository.findByIdAndProjectId(task.getWorkflowId(), projectId)
                .orElseThrow(() -> new RuntimeException("Workflow no encontrado"));

        LocalDateTime now = LocalDateTime.now();

        List<StoredFileInfo> uploadedFiles = new ArrayList<>();

        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    continue;
                }

                try {
                    StoredFileInfo storedFile = s3StorageService.uploadTaskTramiteFile(
                            task.getTicketId(),
                            task.getId(),
                            file,
                            currentUser.getId());

                    uploadedFiles.add(storedFile);
                } catch (IOException e) {
                    throw new RuntimeException("No se pudo subir el archivo a AWS S3");
                }
            }
        }

        task.setStatus(TaskStatus.DONE);
        task.setStartedAt(task.getStartedAt() != null ? task.getStartedAt() : task.getCreatedAt());
        task.setCompletedAt(now);
        task.setSubmittedTramiteData(request != null ? request.getTramiteData() : null);
        task.setUploadedFiles(uploadedFiles);
        workflowTaskRepository.save(task);

        TicketStepHistory history = TicketStepHistory.builder()
                .projectId(projectId)
                .ticketId(ticket.getId())
                .workflowId(task.getWorkflowId())
                .nodeId(task.getNodeId())
                .nodeLabel(task.getNodeLabel())
                .nodeType(task.getNodeType())
                .departmentId(task.getDepartmentId())
                .departmentName(task.getDepartmentName())
                .assignedUserId(task.getAssignedUserId())
                .assignedUserName(task.getAssignedUserName())
                .requiresTramite(task.isRequiresTramite())
                .tramiteTemplateId(task.getTramiteTemplateId())
                .tramiteTemplateName(task.getTramiteTemplateName())
                .decisionResult(request != null ? request.getDecisionResult() : null)
                .submittedTramiteData(task.getSubmittedTramiteData())
                .uploadedFiles(uploadedFiles)
                .startedAt(task.getStartedAt())
                .completedAt(now)
                .build();

        ticketStepHistoryRepository.save(history);

        List<Map<String, Object>> nodes = workflow.getNodes() != null ? workflow.getNodes() : Collections.emptyList();
        List<Map<String, Object>> edges = workflow.getEdges() != null ? workflow.getEdges() : Collections.emptyList();

        Map<String, Object> nextNode;

        if ("decision".equalsIgnoreCase(task.getNodeType())) {
            nextNode = findDecisionTargetNode(task.getNodeId(), request.getDecisionResult(), nodes, edges);
        } else {
            nextNode = findDirectTargetNode(task.getNodeId(), nodes, edges);
        }

        if (nextNode == null) {
            finishTicket(ticket, now);
            return mapTask(task);
        }

        advanceIntoNode(
                ticket,
                workflow,
                nextNode,
                task.getParallelGroupId(),
                task.getForkNodeId(),
                task.getJoinNodeId(),
                task.getBranchSourceNodeId(),
                now);

        return mapTask(task);
    }

    private void finishTicket(Ticket ticket, LocalDateTime now) {
        ticket.setStatus(TicketStatus.COMPLETED);
        ticket.setUpdatedAt(now);
        ticketRepository.save(ticket);
    }

    private String getNodeType(Map<String, Object> node) {
        if (node == null)
            return "";

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) node.get("data");

        return data != null ? String.valueOf(data.getOrDefault("nodeType", "")) : "";
    }

    private TicketTaskInfoResponse mapTicketInfo(Ticket ticket) {
        if (ticket == null) {
            return null;
        }

        return TicketTaskInfoResponse.builder()
                .id(ticket.getId())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .clientName(ticket.getClientName())
                .clientPhone(ticket.getClientPhone())
                .clientEmail(ticket.getClientEmail())
                .clientReference(ticket.getClientReference())
                .status(ticket.getStatus())
                .metadata(ticket.getMetadata())
                .uploadedFiles(ticket.getUploadedFiles())
                .build();
    }

    private Map<String, Object> getNodeById(String nodeId, List<Map<String, Object>> nodes) {
        for (Map<String, Object> node : nodes) {
            if (nodeId.equals(String.valueOf(node.get("id")))) {
                return node;
            }
        }
        return null;
    }

    private Map<String, Object> findDirectTargetNode(
            String sourceNodeId,
            List<Map<String, Object>> nodes,
            List<Map<String, Object>> edges) {

        String targetId = findDirectTargetNodeId(sourceNodeId, edges);
        if (targetId == null)
            return null;

        return getNodeById(targetId, nodes);
    }

    private List<String> findDirectTargetNodeIds(String sourceNodeId, List<Map<String, Object>> edges) {
        List<String> result = new ArrayList<>();

        for (Map<String, Object> edge : edges) {
            String sourceCellId = extractCellId(edge.get("source"));
            if (sourceNodeId.equals(sourceCellId)) {
                String targetCellId = extractCellId(edge.get("target"));
                if (targetCellId != null) {
                    result.add(targetCellId);
                }
            }
        }

        return result;
    }

    private void createOperationalTask(
            Ticket ticket,
            Workflow workflow,
            Map<String, Object> node,
            String parallelGroupId,
            String forkNodeId,
            String joinNodeId,
            String branchSourceNodeId,
            LocalDateTime now) {

        @SuppressWarnings("unchecked")
        Map<String, Object> nodeData = (Map<String, Object>) node.get("data");

        String nodeId = String.valueOf(node.getOrDefault("id", ""));
        String nodeLabel = nodeData != null
                ? String.valueOf(nodeData.getOrDefault("label", node.getOrDefault("label", "Nodo")))
                : String.valueOf(node.getOrDefault("label", "Nodo"));

        String nodeType = nodeData != null ? String.valueOf(nodeData.getOrDefault("nodeType", "")) : "";
        String departmentId = nodeData != null ? String.valueOf(nodeData.getOrDefault("departmentId", "")) : "";
        String departmentName = nodeData != null ? String.valueOf(nodeData.getOrDefault("departmentName", "")) : "";
        String decisionMode = nodeData != null ? String.valueOf(nodeData.getOrDefault("decisionMode", "")) : "";
        String decisionQuestion = nodeData != null ? String.valueOf(nodeData.getOrDefault("decisionQuestion", "")) : "";

        @SuppressWarnings("unchecked")
        List<Map<String, String>> decisionOptions = nodeData != null
                && nodeData.get("decisionOptions") instanceof List<?>
                        ? (List<Map<String, String>>) nodeData.get("decisionOptions")
                        : Collections.emptyList();

        if (departmentId == null || departmentId.isBlank()) {
            throw new RuntimeException("El nodo " + nodeLabel + " no tiene departamento asignado");
        }

        Department department = departmentRepository.findByIdAndProjectId(departmentId, ticket.getProjectId())
                .orElseThrow(() -> new RuntimeException("El departamento del nodo no existe"));

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

        WorkflowTask nextTask = WorkflowTask.builder()
                .projectId(ticket.getProjectId())
                .ticketId(ticket.getId())
                .workflowId(workflow.getId())
                .nodeId(nodeId)
                .nodeLabel(nodeLabel)
                .nodeType(nodeType)
                .departmentId(department.getId())
                .departmentName(
                        departmentName != null && !departmentName.isBlank() ? departmentName : department.getName())
                .assignedUserId(assignedUserId)
                .assignedUserName(assignedUserName)
                .requiresTramite(department.isRequiresTramite())
                .tramiteTemplateId(department.getTramiteTemplateId())
                .tramiteTemplateName(tramiteTemplateName)
                .decisionMode(decisionMode)
                .decisionQuestion(decisionQuestion)
                .decisionOptions(decisionOptions)
                .status(TaskStatus.PENDING)
                .parallelGroupId(parallelGroupId)
                .forkNodeId(forkNodeId)
                .joinNodeId(joinNodeId)
                .branchSourceNodeId(branchSourceNodeId)
                .createdAt(now)
                .build();

        workflowTaskRepository.save(nextTask);

        ticket.setStatus(TicketStatus.IN_PROGRESS);
        ticket.setCurrentNodeId(nodeId);
        ticket.setCurrentDepartmentId(department.getId());
        ticket.setCurrentDepartmentName(department.getName());
        ticket.setUpdatedAt(now);
        ticketRepository.save(ticket);
    }

    private void advanceIntoNode(
            Ticket ticket,
            Workflow workflow,
            Map<String, Object> node,
            String parallelGroupId,
            String forkNodeId,
            String joinNodeId,
            String branchSourceNodeId,
            LocalDateTime now) {

        if (node == null) {
            finishTicket(ticket, now);
            return;
        }

        List<Map<String, Object>> nodes = workflow.getNodes() != null ? workflow.getNodes() : Collections.emptyList();
        List<Map<String, Object>> edges = workflow.getEdges() != null ? workflow.getEdges() : Collections.emptyList();

        String nodeId = String.valueOf(node.get("id"));
        String nodeType = getNodeType(node);

        switch (nodeType.toLowerCase()) {
            case "task":
            case "activity":
            case "decision":
                createOperationalTask(ticket, workflow, node, parallelGroupId, forkNodeId, joinNodeId,
                        branchSourceNodeId, now);
                break;

            case "fork":
                openParallel(ticket, workflow, nodeId, now);
                break;

            case "join":
                arriveToJoin(ticket, workflow, nodeId, parallelGroupId, forkNodeId, branchSourceNodeId, now);
                break;

            case "merge":
            case "start":
                advanceIntoNode(
                        ticket,
                        workflow,
                        findDirectTargetNode(nodeId, nodes, edges),
                        parallelGroupId,
                        forkNodeId,
                        joinNodeId,
                        branchSourceNodeId,
                        now);
                break;

            case "end":
                finishTicket(ticket, now);
                break;

            default:
                throw new RuntimeException("Tipo de nodo no soportado: " + nodeType);
        }
    }

    private void openParallel(Ticket ticket, Workflow workflow, String forkNodeId, LocalDateTime now) {
        List<Map<String, Object>> nodes = workflow.getNodes() != null ? workflow.getNodes() : Collections.emptyList();
        List<Map<String, Object>> edges = workflow.getEdges() != null ? workflow.getEdges() : Collections.emptyList();

        List<String> branchTargets = findDirectTargetNodeIds(forkNodeId, edges);

        if (branchTargets.size() < 2) {
            throw new RuntimeException("El fork debe tener al menos 2 salidas");
        }

        String commonJoinNodeId = findCommonJoinNodeId(branchTargets, nodes, edges);
        String parallelGroupId = UUID.randomUUID().toString();

        ParallelJoinState joinState = ParallelJoinState.builder()
                .projectId(ticket.getProjectId())
                .ticketId(ticket.getId())
                .workflowId(workflow.getId())
                .parallelGroupId(parallelGroupId)
                .forkNodeId(forkNodeId)
                .joinNodeId(commonJoinNodeId)
                .expectedBranches(branchTargets.size())
                .arrivedBranchSourceNodeIds(new ArrayList<>())
                .released(false)
                .createdAt(now)
                .updatedAt(now)
                .build();

        parallelJoinStateRepository.save(joinState);

        for (String branchStartId : branchTargets) {
            Map<String, Object> branchNode = getNodeById(branchStartId, nodes);

            advanceIntoNode(
                    ticket,
                    workflow,
                    branchNode,
                    parallelGroupId,
                    forkNodeId,
                    commonJoinNodeId,
                    branchStartId,
                    now);
        }
    }

    private void arriveToJoin(
            Ticket ticket,
            Workflow workflow,
            String joinNodeId,
            String parallelGroupId,
            String forkNodeId,
            String branchSourceNodeId,
            LocalDateTime now) {

        if (parallelGroupId == null || parallelGroupId.isBlank()) {
            throw new RuntimeException("El join fue alcanzado sin parallelGroupId");
        }

        ParallelJoinState joinState = parallelJoinStateRepository
                .findByTicketIdAndJoinNodeIdAndParallelGroupId(ticket.getId(), joinNodeId, parallelGroupId)
                .orElseThrow(() -> new RuntimeException("No se encontró el estado del join paralelo"));

        if (joinState.isReleased()) {
            return;
        }

        List<String> arrived = joinState.getArrivedBranchSourceNodeIds() != null
                ? joinState.getArrivedBranchSourceNodeIds()
                : new ArrayList<>();

        if (branchSourceNodeId != null && !arrived.contains(branchSourceNodeId)) {
            arrived.add(branchSourceNodeId);
        }

        joinState.setArrivedBranchSourceNodeIds(arrived);
        joinState.setUpdatedAt(now);

        if (arrived.size() < joinState.getExpectedBranches()) {
            parallelJoinStateRepository.save(joinState);
            return;
        }

        joinState.setReleased(true);
        joinState.setReleasedAt(now);
        parallelJoinStateRepository.save(joinState);

        List<Map<String, Object>> nodes = workflow.getNodes() != null ? workflow.getNodes() : Collections.emptyList();
        List<Map<String, Object>> edges = workflow.getEdges() != null ? workflow.getEdges() : Collections.emptyList();

        Map<String, Object> nextNode = findDirectTargetNode(joinNodeId, nodes, edges);

        if (nextNode == null) {
            finishTicket(ticket, now);
            return;
        }

        advanceIntoNode(ticket, workflow, nextNode, null, null, null, null, now);
    }

    public void startWorkflowForTicket(Ticket ticket, Workflow workflow) {
        LocalDateTime now = LocalDateTime.now();

        List<Map<String, Object>> nodes = workflow.getNodes() != null ? workflow.getNodes() : Collections.emptyList();
        List<Map<String, Object>> edges = workflow.getEdges() != null ? workflow.getEdges() : Collections.emptyList();

        Map<String, Object> startNode = null;

        for (Map<String, Object> node : nodes) {
            String nodeType = getNodeType(node);
            if ("start".equalsIgnoreCase(nodeType)) {
                startNode = node;
                break;
            }
        }

        if (startNode == null) {
            throw new RuntimeException("El workflow no tiene nodo start");
        }

        String startNodeId = String.valueOf(startNode.get("id"));
        Map<String, Object> firstNode = findDirectTargetNode(startNodeId, nodes, edges);

        if (firstNode == null) {
            throw new RuntimeException("El nodo start no tiene un siguiente nodo");
        }

        advanceIntoNode(ticket, workflow, firstNode, null, null, null, null, now);
    }

    private String findCommonJoinNodeId(
            List<String> branchTargets,
            List<Map<String, Object>> nodes,
            List<Map<String, Object>> edges) {

        Set<String> joinIds = new HashSet<>();

        for (String branchStartId : branchTargets) {
            String joinId = findFirstJoinAhead(branchStartId, nodes, edges, new HashSet<>());
            if (joinId == null) {
                throw new RuntimeException("Una rama paralela no llega a ningún join");
            }
            joinIds.add(joinId);
        }

        if (joinIds.size() != 1) {
            throw new RuntimeException("Todas las ramas del fork deben converger en el mismo join");
        }

        return joinIds.iterator().next();
    }

    private String findFirstJoinAhead(
            String currentNodeId,
            List<Map<String, Object>> nodes,
            List<Map<String, Object>> edges,
            Set<String> visited) {

        if (currentNodeId == null || visited.contains(currentNodeId)) {
            return null;
        }

        visited.add(currentNodeId);

        Map<String, Object> node = getNodeById(currentNodeId, nodes);
        if (node == null) {
            return null;
        }

        String nodeType = getNodeType(node);
        if ("join".equalsIgnoreCase(nodeType)) {
            return currentNodeId;
        }

        List<String> nextIds = findDirectTargetNodeIds(currentNodeId, edges);

        for (String nextId : nextIds) {
            String found = findFirstJoinAhead(nextId, nodes, edges, visited);
            if (found != null) {
                return found;
            }
        }

        return null;
    }

    private Map<String, Object> findDecisionTargetNode(
            String decisionNodeId,
            String decisionResult,
            List<Map<String, Object>> nodes,
            List<Map<String, Object>> edges) {
        Map<String, Map<String, Object>> nodeMap = nodes.stream()
                .collect(Collectors.toMap(node -> String.valueOf(node.get("id")), node -> node, (a, b) -> a));

        for (Map<String, Object> edge : edges) {
            String sourceCellId = extractCellId(edge.get("source"));
            if (!decisionNodeId.equals(sourceCellId)) {
                continue;
            }

            String conditionValue = edge.get("conditionValue") != null
                    ? String.valueOf(edge.get("conditionValue"))
                    : null;

            if (conditionValue != null && conditionValue.equalsIgnoreCase(decisionResult)) {
                String targetCellId = extractCellId(edge.get("target"));
                return nodeMap.get(targetCellId);
            }
        }

        throw new RuntimeException("No se encontró una ruta para la decisión seleccionada");
    }

    private Map<String, Object> findNextOperationalNode(
            String currentNodeId,
            List<Map<String, Object>> nodes,
            List<Map<String, Object>> edges) {
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