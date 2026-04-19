package com.parcial1.service;

import com.parcial1.dto.CreateWorkflowRequest;
import com.parcial1.dto.MessageResponse;
import com.parcial1.dto.WorkflowDiagramRequest;
import com.parcial1.dto.WorkflowDiagramResponse;
import com.parcial1.dto.WorkflowStatusRequest;
import com.parcial1.dto.WorkflowSummaryResponse;
import com.parcial1.model.ProjectMember;
import com.parcial1.model.ProjectRole;
import com.parcial1.model.TicketStatus;
import com.parcial1.model.User;
import com.parcial1.model.Workflow;
import com.parcial1.model.WorkflowStatus;
import com.parcial1.repository.DepartmentRepository;
import com.parcial1.repository.ProjectMemberRepository;
import com.parcial1.repository.TicketRepository;
import com.parcial1.repository.UserRepository;
import com.parcial1.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

// import com.parcial1.model.TicketStatus;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final DepartmentRepository departmentRepository;

    private void validateWorkflowForPublishing(Workflow workflow, String projectId) {
        List<String> errors = new ArrayList<>();

        List<Map<String, Object>> nodes = workflow.getNodes() != null ? workflow.getNodes() : Collections.emptyList();
        List<Map<String, Object>> edges = workflow.getEdges() != null ? workflow.getEdges() : Collections.emptyList();

        if (nodes.isEmpty()) {
            errors.add("El workflow no tiene nodos");
        }

        if (edges.isEmpty()) {
            errors.add("El workflow no tiene conexiones");
        }

        Map<String, Map<String, Object>> nodeMap = new HashMap<>();
        for (Map<String, Object> node : nodes) {
            String nodeId = String.valueOf(node.get("id"));
            if (nodeId == null || nodeId.isBlank()) {
                errors.add("Existe un nodo sin id");
                continue;
            }

            if (nodeMap.containsKey(nodeId)) {
                errors.add("Existe un id de nodo repetido: " + nodeId);
                continue;
            }

            nodeMap.put(nodeId, node);
        }

        Map<String, Integer> incomingCount = new HashMap<>();
        Map<String, Integer> outgoingCount = new HashMap<>();
        Map<String, List<String>> outgoingMap = new HashMap<>();
        Map<String, List<String>> incomingMap = new HashMap<>();

        for (String nodeId : nodeMap.keySet()) {
            incomingCount.put(nodeId, 0);
            outgoingCount.put(nodeId, 0);
            outgoingMap.put(nodeId, new ArrayList<>());
            incomingMap.put(nodeId, new ArrayList<>());
        }

        for (Map<String, Object> edge : edges) {
            String sourceId = extractCellId(edge.get("source"));
            String targetId = extractCellId(edge.get("target"));

            if (sourceId == null || sourceId.isBlank()) {
                errors.add("Existe una conexión sin source");
                continue;
            }

            if (targetId == null || targetId.isBlank()) {
                errors.add("Existe una conexión sin target");
                continue;
            }

            if (!nodeMap.containsKey(sourceId)) {
                errors.add("Existe una conexión cuyo origen no existe: " + sourceId);
                continue;
            }

            if (!nodeMap.containsKey(targetId)) {
                errors.add("Existe una conexión cuyo destino no existe: " + targetId);
                continue;
            }

            outgoingCount.put(sourceId, outgoingCount.get(sourceId) + 1);
            incomingCount.put(targetId, incomingCount.get(targetId) + 1);
            outgoingMap.get(sourceId).add(targetId);
            incomingMap.get(targetId).add(sourceId);
        }

        List<Map<String, Object>> startNodes = nodes.stream()
                .filter(node -> "start".equalsIgnoreCase(getNodeType(node)))
                .toList();

        List<Map<String, Object>> endNodes = nodes.stream()
                .filter(node -> "end".equalsIgnoreCase(getNodeType(node)))
                .toList();

        if (startNodes.size() != 1) {
            errors.add("Debe existir exactamente 1 nodo start");
        }

        if (endNodes.size() != 1) {
            errors.add("Debe existir exactamente 1 nodo end");
        }

        for (Map<String, Object> node : nodes) {
            String nodeId = String.valueOf(node.get("id"));
            String nodeType = getNodeType(node).toLowerCase();
            String nodeLabel = getNodeLabel(node);

            int incoming = incomingCount.getOrDefault(nodeId, 0);
            int outgoing = outgoingCount.getOrDefault(nodeId, 0);

            switch (nodeType) {
                case "start":
                    if (incoming != 0) {
                        errors.add("El nodo start \"" + nodeLabel + "\" no debe tener entradas");
                    }
                    if (outgoing != 1) {
                        errors.add("El nodo start \"" + nodeLabel + "\" debe tener exactamente 1 salida");
                    }
                    break;

                case "end":
                    if (incoming < 1) {
                        errors.add("El nodo end \"" + nodeLabel + "\" debe tener al menos 1 entrada");
                    }
                    if (outgoing != 0) {
                        errors.add("El nodo end \"" + nodeLabel + "\" no debe tener salidas");
                    }
                    break;

                case "task":
                case "activity":
                case "decision":
                    validateDepartmentAssignment(node, projectId, errors);

                    if (incoming < 1) {
                        errors.add("El nodo \"" + nodeLabel + "\" debe tener al menos 1 entrada");
                    }
                    if (outgoing < 1) {
                        errors.add("El nodo \"" + nodeLabel + "\" debe tener al menos 1 salida");
                    }

                    if ("decision".equals(nodeType)) {
                        if (outgoing < 2) {
                            errors.add("El nodo de decisión \"" + nodeLabel + "\" debe tener al menos 2 salidas");
                        }

                        for (Map<String, Object> edge : edges) {
                            String sourceId = extractCellId(edge.get("source"));
                            if (nodeId.equals(sourceId)) {
                                Object conditionValue = edge.get("conditionValue");
                                if (conditionValue == null || String.valueOf(conditionValue).isBlank()) {
                                    errors.add("La decisión \"" + nodeLabel + "\" tiene una salida sin condición");
                                }
                            }
                        }
                    }
                    break;

                case "fork":
                    if (incoming < 1) {
                        errors.add("El fork \"" + nodeLabel + "\" debe tener al menos 1 entrada");
                    }
                    if (outgoing < 2) {
                        errors.add("El fork \"" + nodeLabel + "\" debe tener al menos 2 salidas");
                    } else {
                        try {
                            findCommonJoinNodeId(outgoingMap.getOrDefault(nodeId, Collections.emptyList()), nodes,
                                    outgoingMap);
                        } catch (RuntimeException ex) {
                            errors.add("El fork \"" + nodeLabel + "\": " + ex.getMessage());
                        }
                    }
                    break;

                case "join":
                    if (incoming < 2) {
                        errors.add("El join \"" + nodeLabel + "\" debe tener al menos 2 entradas");
                    }
                    if (outgoing != 1) {
                        errors.add("El join \"" + nodeLabel + "\" debe tener exactamente 1 salida");
                    }
                    break;

                case "merge":
                    if (incoming < 2) {
                        errors.add("El merge \"" + nodeLabel + "\" debe tener al menos 2 entradas");
                    }
                    if (outgoing != 1) {
                        errors.add("El merge \"" + nodeLabel + "\" debe tener exactamente 1 salida");
                    }
                    break;

                default:
                    if (nodeType == null || nodeType.isBlank()) {
                        errors.add("El nodo \"" + nodeLabel + "\" no tiene tipo de nodo");
                    } else {
                        errors.add("El nodo \"" + nodeLabel + "\" tiene un tipo no soportado: " + nodeType);
                    }
                    break;
            }
        }

        if (startNodes.size() == 1) {
            String startId = String.valueOf(startNodes.get(0).get("id"));
            Set<String> reachableFromStart = collectReachableNodes(startId, outgoingMap);

            for (Map<String, Object> node : nodes) {
                String nodeId = String.valueOf(node.get("id"));
                String nodeLabel = getNodeLabel(node);

                if (!reachableFromStart.contains(nodeId)) {
                    errors.add("El nodo \"" + nodeLabel + "\" está desconectado del start");
                }
            }
        }

        if (endNodes.size() == 1) {
            String endId = String.valueOf(endNodes.get(0).get("id"));
            Set<String> reachableToEnd = collectReachableNodesReverse(endId, incomingMap);

            for (Map<String, Object> node : nodes) {
                String nodeId = String.valueOf(node.get("id"));
                String nodeLabel = getNodeLabel(node);

                if (!reachableToEnd.contains(nodeId)) {
                    errors.add("El nodo \"" + nodeLabel + "\" no llega al end");
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new RuntimeException("No se puede poner en producción el workflow por los siguientes errores:\n- "
                    + String.join("\n- ", errors));
        }
    }

    private void validateDepartmentAssignment(Map<String, Object> node, String projectId, List<String> errors) {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) node.get("data");

        String nodeLabel = getNodeLabel(node);
        String departmentId = data != null ? String.valueOf(data.getOrDefault("departmentId", "")) : "";

        if (departmentId == null || departmentId.isBlank()) {
            errors.add("El nodo \"" + nodeLabel + "\" no tiene departamento asignado");
            return;
        }

        boolean exists = departmentRepository.findByIdAndProjectId(departmentId, projectId).isPresent();
        if (!exists) {
            errors.add("El nodo \"" + nodeLabel + "\" tiene un departamento inexistente");
        }
    }

    private String getNodeType(Map<String, Object> node) {
        if (node == null)
            return "";

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) node.get("data");

        return data != null ? String.valueOf(data.getOrDefault("nodeType", "")) : "";
    }

    private String getNodeLabel(Map<String, Object> node) {
        if (node == null)
            return "Nodo";

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) node.get("data");

        if (data != null && data.get("label") != null && !String.valueOf(data.get("label")).isBlank()) {
            return String.valueOf(data.get("label"));
        }

        Object rawLabel = node.get("label");
        if (rawLabel != null && !String.valueOf(rawLabel).isBlank()) {
            return String.valueOf(rawLabel);
        }

        return "Nodo";
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

    private Set<String> collectReachableNodes(String startId, Map<String, List<String>> outgoingMap) {
        Set<String> visited = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(startId);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (!visited.add(current)) {
                continue;
            }

            for (String next : outgoingMap.getOrDefault(current, Collections.emptyList())) {
                if (!visited.contains(next)) {
                    queue.add(next);
                }
            }
        }

        return visited;
    }

    private Set<String> collectReachableNodesReverse(String endId, Map<String, List<String>> incomingMap) {
        Set<String> visited = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(endId);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (!visited.add(current)) {
                continue;
            }

            for (String prev : incomingMap.getOrDefault(current, Collections.emptyList())) {
                if (!visited.contains(prev)) {
                    queue.add(prev);
                }
            }
        }

        return visited;
    }

    private String findCommonJoinNodeId(
            List<String> branchTargets,
            List<Map<String, Object>> nodes,
            Map<String, List<String>> outgoingMap) {

        Set<String> joinIds = new HashSet<>();

        for (String branchStartId : branchTargets) {
            String joinId = findFirstJoinAhead(branchStartId, nodes, outgoingMap, new HashSet<>());
            if (joinId == null) {
                throw new RuntimeException("una rama no llega a ningún join");
            }
            joinIds.add(joinId);
        }

        if (joinIds.size() != 1) {
            throw new RuntimeException("las ramas no convergen en un mismo join");
        }

        return joinIds.iterator().next();
    }

    private String findFirstJoinAhead(
            String currentNodeId,
            List<Map<String, Object>> nodes,
            Map<String, List<String>> outgoingMap,
            Set<String> visited) {

        if (currentNodeId == null || visited.contains(currentNodeId)) {
            return null;
        }

        visited.add(currentNodeId);

        Map<String, Object> currentNode = nodes.stream()
                .filter(node -> currentNodeId.equals(String.valueOf(node.get("id"))))
                .findFirst()
                .orElse(null);

        if (currentNode == null) {
            return null;
        }

        if ("join".equalsIgnoreCase(getNodeType(currentNode))) {
            return currentNodeId;
        }

        for (String nextId : outgoingMap.getOrDefault(currentNodeId, Collections.emptyList())) {
            String found = findFirstJoinAhead(nextId, nodes, outgoingMap, visited);
            if (found != null) {
                return found;
            }
        }

        return null;
    }

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
                        .status(workflow.getStatus() != null ? workflow.getStatus().name() : "DRAFT")
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
                .status(WorkflowStatus.DRAFT)
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
                .status(workflow.getStatus() != null ? workflow.getStatus().name() : "DRAFT")
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
        if (workflow.getStatus() == WorkflowStatus.PUBLISHED) {
            throw new RuntimeException("El workflow está en producción y no puede editarse");
        }

        workflowRepository.save(workflow);

        return new MessageResponse("Workflow guardado correctamente");
    }

    public WorkflowSummaryResponse updateWorkflowStatus(String projectId, String workflowId,
            WorkflowStatusRequest request) {
        User currentUser = getCurrentUser();
        ProjectMember membership = getMembership(projectId, currentUser.getId());
        validateAdmin(membership);

        Workflow workflow = workflowRepository.findByIdAndProjectId(workflowId, projectId)
                .orElseThrow(() -> new RuntimeException("Workflow no encontrado"));

        if (request.getStatus() == WorkflowStatus.DRAFT) {
            long activeTicketsCount = ticketRepository.countByWorkflowIdAndStatusIn(
                    workflowId,
                    List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS));

            if (activeTicketsCount > 0) {
                throw new RuntimeException("No puedes volver a desarrollo porque este workflow tiene tickets activos");
            }
        }

        if (request.getStatus() == WorkflowStatus.PUBLISHED) {
            validateWorkflowForPublishing(workflow, projectId);
        }

        workflow.setStatus(request.getStatus());
        workflow.setUpdatedAt(LocalDateTime.now());

        Workflow saved = workflowRepository.save(workflow);

        return WorkflowSummaryResponse.builder()
                .id(saved.getId())
                .projectId(saved.getProjectId())
                .name(saved.getName())
                .description(saved.getDescription())
                .nodesCount(saved.getNodes() != null ? saved.getNodes().size() : 0)
                .edgesCount(saved.getEdges() != null ? saved.getEdges().size() : 0)
                .status(saved.getStatus() != null ? saved.getStatus().name() : "DRAFT")
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .build();
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