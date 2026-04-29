package com.parcial1.service;

import com.parcial1.dto.DepartmentKpiResponse;
import com.parcial1.dto.ProjectKpiResponse;
import com.parcial1.model.TaskStatus;
import com.parcial1.model.WorkflowTask;
import com.parcial1.repository.WorkflowTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectKpiService {

    private final WorkflowTaskRepository workflowTaskRepository;

    public ProjectKpiResponse getProjectKpis(String projectId, int thresholdTickets, int thresholdDays) {
        LocalDateTime now = LocalDateTime.now();

        List<TaskStatus> activeStatuses = List.of(
                TaskStatus.PENDING,
                TaskStatus.IN_PROGRESS);

        List<WorkflowTask> activeTasks = workflowTaskRepository
                .findByProjectIdAndStatusInOrderByCreatedAtDesc(projectId, activeStatuses);

        Map<String, List<WorkflowTask>> tasksByDepartment = activeTasks.stream()
                .filter(task -> task.getDepartmentId() != null && !task.getDepartmentId().isBlank())
                .collect(Collectors.groupingBy(WorkflowTask::getDepartmentId));

        List<DepartmentKpiResponse> departments = tasksByDepartment.entrySet()
                .stream()
                .map(entry -> buildDepartmentKpi(
                        entry.getKey(),
                        entry.getValue(),
                        now,
                        thresholdTickets,
                        thresholdDays))
                .sorted(Comparator
                        .comparing(DepartmentKpiResponse::isBottleneck).reversed()
                        .thenComparing(DepartmentKpiResponse::getDelayedTickets, Comparator.reverseOrder())
                        .thenComparing(DepartmentKpiResponse::getActiveTickets, Comparator.reverseOrder()))
                .toList();

        long totalDelayedTickets = departments.stream()
                .mapToLong(DepartmentKpiResponse::getDelayedTickets)
                .sum();

        long totalBottleneckDepartments = departments.stream()
                .filter(DepartmentKpiResponse::isBottleneck)
                .count();

        return ProjectKpiResponse.builder()
                .projectId(projectId)
                .generatedAt(now)
                .totalActiveTickets(activeTasks.size())
                .totalDelayedTickets(totalDelayedTickets)
                .totalBottleneckDepartments(totalBottleneckDepartments)
                .thresholdTickets(thresholdTickets)
                .thresholdDays(thresholdDays)
                .departments(departments)
                .build();
    }

    private DepartmentKpiResponse buildDepartmentKpi(
            String departmentId,
            List<WorkflowTask> tasks,
            LocalDateTime now,
            int thresholdTickets,
            int thresholdDays) {
        String departmentName = tasks.stream()
                .map(WorkflowTask::getDepartmentName)
                .filter(Objects::nonNull)
                .filter(name -> !name.isBlank())
                .findFirst()
                .orElse("Departamento sin nombre");

        List<Long> ageHours = tasks.stream()
                .map(task -> calculateAgeHours(task, now))
                .toList();

        long delayedTickets = ageHours.stream()
                .filter(hours -> hours >= thresholdDays * 24L)
                .count();

        double averageAgeHours = ageHours.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);

        long oldestTicketAgeHours = ageHours.stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(0);

        long uniqueTickets = tasks.stream()
                .map(WorkflowTask::getTicketId)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        boolean bottleneck = delayedTickets >= thresholdTickets;

        String severity = calculateSeverity(delayedTickets, thresholdTickets);

        String message = buildMessage(
                departmentName,
                tasks.size(),
                uniqueTickets,
                delayedTickets,
                thresholdDays,
                bottleneck,
                severity,
                averageAgeHours);

        return DepartmentKpiResponse.builder()
                .departmentId(departmentId)
                .departmentName(departmentName)
                .activeTickets(tasks.size())
                .uniqueTickets(uniqueTickets)
                .delayedTickets(delayedTickets)
                .averageAgeHours(roundOneDecimal(averageAgeHours))
                .oldestTicketAgeHours(oldestTicketAgeHours)
                .thresholdTickets(thresholdTickets)
                .thresholdDays(thresholdDays)
                .bottleneck(bottleneck)
                .severity(severity)
                .message(message)
                .build();
    }

    private long calculateAgeHours(WorkflowTask task, LocalDateTime now) {
        LocalDateTime start = task.getCreatedAt();

        if (start == null) {
            return 0;
        }

        return Math.max(0, Duration.between(start, now).toHours());
    }

    private String calculateSeverity(long delayedTickets, int thresholdTickets) {
        if (thresholdTickets <= 0) {
            return "LOW";
        }

        if (delayedTickets >= thresholdTickets * 2L) {
            return "HIGH";
        }

        if (delayedTickets >= thresholdTickets) {
            return "MEDIUM";
        }

        return "LOW";
    }

    private String buildMessage(
            String departmentName,
            long activeTasks,
            long uniqueTickets,
            long delayedTickets,
            int thresholdDays,
            boolean bottleneck,
            String severity,
            double averageAgeHours) {
        double averageDays = roundOneDecimal(averageAgeHours / 24.0);

        if (bottleneck) {
            return departmentName + " presenta cuello de botella: tiene "
                    + delayedTickets + " tareas/tickets con "
                    + thresholdDays + " días o más, de un total de "
                    + activeTasks + " tareas activas y "
                    + uniqueTickets + " tickets únicos. Promedio de espera: "
                    + averageDays + " días. Nivel: " + severity + ".";
        }

        return departmentName + " no presenta cuello de botella: tiene "
                + delayedTickets + " tareas/tickets con "
                + thresholdDays + " días o más, de un total de "
                + activeTasks + " tareas activas y "
                + uniqueTickets + " tickets únicos. Promedio de espera: "
                + averageDays + " días.";
    }

    private double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}