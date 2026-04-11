package com.parcial1.controller;

import com.parcial1.dto.MessageResponse;
import com.parcial1.dto.WorkflowDiagramRequest;
import com.parcial1.dto.WorkflowDiagramResponse;
import com.parcial1.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{projectId}/workflow")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    @GetMapping
    public ResponseEntity<WorkflowDiagramResponse> getWorkflow(@PathVariable String projectId) {
        return ResponseEntity.ok(workflowService.getWorkflow(projectId));
    }

    @PutMapping
    public ResponseEntity<MessageResponse> saveWorkflow(
            @PathVariable String projectId,
            @RequestBody WorkflowDiagramRequest request
    ) {
        return ResponseEntity.ok(workflowService.saveWorkflow(projectId, request));
    }
}