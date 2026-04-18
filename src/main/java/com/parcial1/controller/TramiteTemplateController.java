package com.parcial1.controller;

import com.parcial1.dto.CreateTramiteRequest;
import com.parcial1.dto.MessageResponse;
import com.parcial1.dto.TramiteResponse;
import com.parcial1.dto.UpdateTramiteRequest;
import com.parcial1.service.TramiteTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects/{projectId}/tramites")
public class TramiteTemplateController {

    private final TramiteTemplateService tramiteTemplateService;

    @GetMapping
    public ResponseEntity<List<TramiteResponse>> getTramites(@PathVariable String projectId) {
        return ResponseEntity.ok(tramiteTemplateService.getTramites(projectId));
    }

    @GetMapping("/{tramiteId}")
    public ResponseEntity<TramiteResponse> getTramiteById(
            @PathVariable String projectId,
            @PathVariable String tramiteId
    ) {
        return ResponseEntity.ok(tramiteTemplateService.getTramiteById(projectId, tramiteId));
    }

    @PostMapping
    public ResponseEntity<TramiteResponse> createTramite(
            @PathVariable String projectId,
            @Valid @RequestBody CreateTramiteRequest request
    ) {
        return ResponseEntity.ok(tramiteTemplateService.createTramite(projectId, request));
    }

    @PutMapping("/{tramiteId}")
    public ResponseEntity<TramiteResponse> updateTramite(
            @PathVariable String projectId,
            @PathVariable String tramiteId,
            @RequestBody UpdateTramiteRequest request
    ) {
        return ResponseEntity.ok(tramiteTemplateService.updateTramite(projectId, tramiteId, request));
    }

    @DeleteMapping("/{tramiteId}")
    public ResponseEntity<MessageResponse> deleteTramite(
            @PathVariable String projectId,
            @PathVariable String tramiteId
    ) {
        return ResponseEntity.ok(tramiteTemplateService.deleteTramite(projectId, tramiteId));
    }
}