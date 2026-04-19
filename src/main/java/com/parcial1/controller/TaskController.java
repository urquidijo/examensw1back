package com.parcial1.controller;

import com.parcial1.dto.*;
import com.parcial1.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping("/task-board/departments")
    public ResponseEntity<List<DepartmentTaskBoardResponse>> getTaskBoardDepartments(
            @PathVariable String projectId
    ) {
        return ResponseEntity.ok(taskService.getTaskBoardDepartments(projectId));
    }

    @GetMapping("/departments/{departmentId}/my-tasks")
    public ResponseEntity<List<WorkflowTaskResponse>> getMyDepartmentTasks(
            @PathVariable String projectId,
            @PathVariable String departmentId
    ) {
        return ResponseEntity.ok(taskService.getMyDepartmentTasks(projectId, departmentId));
    }

    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<WorkflowTaskResponse> getTaskDetail(
            @PathVariable String projectId,
            @PathVariable String taskId
    ) {
        return ResponseEntity.ok(taskService.getTaskDetail(projectId, taskId));
    }

    @PostMapping("/tasks/{taskId}/complete")
    public ResponseEntity<WorkflowTaskResponse> completeTask(
            @PathVariable String projectId,
            @PathVariable String taskId,
            @RequestBody CompleteTaskRequest request
    ) {
        return ResponseEntity.ok(taskService.completeTask(projectId, taskId, request));
    }

    @GetMapping("/departments/{departmentId}/completed-history")
    public ResponseEntity<List<TicketStepHistoryResponse>> getDepartmentCompletedHistory(
            @PathVariable String projectId,
            @PathVariable String departmentId
    ) {
        return ResponseEntity.ok(taskService.getDepartmentCompletedHistory(projectId, departmentId));
    }
}