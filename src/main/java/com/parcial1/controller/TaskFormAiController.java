package com.parcial1.controller;

import com.parcial1.dto.FormAiFillRequest;
import com.parcial1.dto.FormAiFillResponse;
import com.parcial1.service.TaskFormAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{projectId}/tasks/{taskId}/form-ai")
@RequiredArgsConstructor
public class TaskFormAiController {

    private final TaskFormAiService taskFormAiService;

    @PostMapping("/fill")
    public ResponseEntity<FormAiFillResponse> fillForm(
            @PathVariable String projectId,
            @PathVariable String taskId,
            @RequestBody FormAiFillRequest request
    ) {
        return ResponseEntity.ok(
                taskFormAiService.fillForm(projectId, taskId, request)
        );
    }
}