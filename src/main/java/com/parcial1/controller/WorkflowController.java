package com.parcial1.controller;

import com.parcial1.dto.CreateWorkflowRequest;
import com.parcial1.dto.MessageResponse;
import com.parcial1.dto.WorkflowDiagramRequest;
import com.parcial1.dto.WorkflowDiagramResponse;
import com.parcial1.dto.WorkflowStatusRequest;
import com.parcial1.dto.WorkflowSummaryResponse;
import com.parcial1.service.WorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    @GetMapping
    public ResponseEntity<List<WorkflowSummaryResponse>> getWorkflows(@PathVariable String projectId) {
        return ResponseEntity.ok(workflowService.getWorkflows(projectId));
    }

    @PostMapping
    public ResponseEntity<WorkflowSummaryResponse> createWorkflow(
            @PathVariable String projectId,
            @Valid @RequestBody CreateWorkflowRequest request) {
        return ResponseEntity.ok(workflowService.createWorkflow(projectId, request));
    }

    @GetMapping("/{workflowId}")
    public ResponseEntity<WorkflowDiagramResponse> getWorkflow(
            @PathVariable String projectId,
            @PathVariable String workflowId) {
        return ResponseEntity.ok(workflowService.getWorkflow(projectId, workflowId));
    }

    @PutMapping("/{workflowId}")
    public ResponseEntity<MessageResponse> saveWorkflow(
            @PathVariable String projectId,
            @PathVariable String workflowId,
            @RequestBody WorkflowDiagramRequest request) {
        return ResponseEntity.ok(workflowService.saveWorkflow(projectId, workflowId, request));
    }

    @PatchMapping("/{workflowId}/status")
    public ResponseEntity<WorkflowSummaryResponse> updateWorkflowStatus(
            @PathVariable String projectId,
            @PathVariable String workflowId,
            @Valid @RequestBody WorkflowStatusRequest request) {
        return ResponseEntity.ok(workflowService.updateWorkflowStatus(projectId, workflowId, request));
    }

    @DeleteMapping("/{workflowId}")
    public ResponseEntity<MessageResponse> deleteWorkflow(
            @PathVariable String projectId,
            @PathVariable String workflowId) {
        return ResponseEntity.ok(workflowService.deleteWorkflow(projectId, workflowId));
    }
}