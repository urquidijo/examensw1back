package com.parcial1.controller;

import com.parcial1.dto.ai.WorkflowAiCommandRequest;
import com.parcial1.dto.ai.WorkflowAiGraphResponse;
import com.parcial1.service.GeminiWorkflowAiService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects/{projectId}/workflows/{workflowId}")
public class WorkflowAiController {

    private final GeminiWorkflowAiService geminiWorkflowAiService;

    @PostMapping("/ai-command")
    public ResponseEntity<?> aiCommand(
        @PathVariable String projectId,
        @PathVariable String workflowId,
        @RequestBody WorkflowAiCommandRequest request
    ) {
        try {
            WorkflowAiGraphResponse response =
                geminiWorkflowAiService.processCommand(projectId, workflowId, request);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));

        } catch (IllegalStateException e) {
            HttpStatus status = e.getMessage() != null && e.getMessage().contains("503")
                ? HttpStatus.SERVICE_UNAVAILABLE
                : HttpStatus.INTERNAL_SERVER_ERROR;

            return ResponseEntity.status(status).body(Map.of(
                "error", e.getMessage()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Error interno procesando comando con IA: " + e.getMessage()
            ));
        }
    }
}