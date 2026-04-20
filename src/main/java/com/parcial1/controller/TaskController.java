package com.parcial1.controller;

import com.parcial1.dto.*;
import com.parcial1.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping("/task-board/departments")
    public ResponseEntity<List<DepartmentTaskBoardResponse>> getTaskBoardDepartments(
            @PathVariable String projectId) {
        return ResponseEntity.ok(taskService.getTaskBoardDepartments(projectId));
    }

    @GetMapping("/departments/{departmentId}/my-tasks")
    public ResponseEntity<List<WorkflowTaskResponse>> getMyDepartmentTasks(
            @PathVariable String projectId,
            @PathVariable String departmentId) {
        return ResponseEntity.ok(taskService.getMyDepartmentTasks(projectId, departmentId));
    }

    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<WorkflowTaskResponse> getTaskDetail(
            @PathVariable String projectId,
            @PathVariable String taskId) {
        return ResponseEntity.ok(taskService.getTaskDetail(projectId, taskId));
    }

    @PostMapping(value = "/tasks/{taskId}/complete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<WorkflowTaskResponse> completeTask(
            @PathVariable String projectId,
            @PathVariable String taskId,
            @RequestPart("payload") CompleteTaskRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) throws IOException {
        return ResponseEntity.ok(taskService.completeTask(projectId, taskId, request, files));
    }

    @GetMapping("/departments/{departmentId}/completed-history")
    public ResponseEntity<List<TicketStepHistoryResponse>> getDepartmentCompletedHistory(
            @PathVariable String projectId,
            @PathVariable String departmentId) {
        return ResponseEntity.ok(taskService.getDepartmentCompletedHistory(projectId, departmentId));
    }

    @GetMapping("/completed-history/{historyId}/files/download")
    public ResponseEntity<byte[]> downloadCompletedHistoryFile(
            @PathVariable String projectId,
            @PathVariable String historyId,
            @RequestParam String key) {
        TaskFileDownloadResponse file = taskService.downloadCompletedHistoryFile(projectId, historyId, key);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + file.getOriginalName() + "\"")
                .header("Content-Type",
                        file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                .body(file.getContent());
    }

    @GetMapping("/tickets/{ticketId}/monitor")
    public ResponseEntity<TicketMonitorResponse> getTicketMonitor(
            @PathVariable String projectId,
            @PathVariable String ticketId) {
        return ResponseEntity.ok(taskService.getTicketMonitor(projectId, ticketId));
    }
}