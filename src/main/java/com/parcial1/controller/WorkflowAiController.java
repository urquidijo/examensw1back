package com.parcial1.controller;

import com.parcial1.dto.ai.WorkflowAiCommandRequest;
import com.parcial1.dto.ai.WorkflowAiResponse;
import com.parcial1.service.GeminiWorkflowAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects/{projectId}/workflows/{workflowId}")
public class WorkflowAiController {

    private final GeminiWorkflowAiService geminiWorkflowAiService;

    @PostMapping("/ai-command")
    public WorkflowAiResponse aiCommand(
        @PathVariable String projectId,
        @PathVariable String workflowId,
        @RequestBody WorkflowAiCommandRequest request
    ) {
        return geminiWorkflowAiService.processCommand(projectId, workflowId, request);
    }
}