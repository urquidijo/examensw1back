package com.parcial1.controller;

import com.parcial1.dto.ProjectAssistantChatRequest;
import com.parcial1.dto.ProjectAssistantChatResponse;
import com.parcial1.service.ProjectAssistantAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{projectId}/assistant")
@RequiredArgsConstructor
public class ProjectAssistantController {

    private final ProjectAssistantAiService projectAssistantAiService;

    @PostMapping("/chat")
    public ResponseEntity<ProjectAssistantChatResponse> chat(
            @PathVariable String projectId,
            @RequestBody ProjectAssistantChatRequest request
    ) {
        return ResponseEntity.ok(
                projectAssistantAiService.chat(projectId, request)
        );
    }
}