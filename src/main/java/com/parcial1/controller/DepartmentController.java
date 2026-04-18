package com.parcial1.controller;

import com.parcial1.dto.CreateDepartmentRequest;
import com.parcial1.dto.DepartmentResponse;
import com.parcial1.dto.MessageResponse;
import com.parcial1.dto.UpdateDepartmentRequest;
import com.parcial1.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects/{projectId}/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    public ResponseEntity<List<DepartmentResponse>> getDepartments(@PathVariable String projectId) {
        return ResponseEntity.ok(departmentService.getDepartments(projectId));
    }

    @GetMapping("/{departmentId}")
    public ResponseEntity<DepartmentResponse> getDepartmentById(
            @PathVariable String projectId,
            @PathVariable String departmentId
    ) {
        return ResponseEntity.ok(departmentService.getDepartmentById(projectId, departmentId));
    }

    @PostMapping
    public ResponseEntity<DepartmentResponse> createDepartment(
            @PathVariable String projectId,
            @Valid @RequestBody CreateDepartmentRequest request
    ) {
        return ResponseEntity.ok(departmentService.createDepartment(projectId, request));
    }

    @PutMapping("/{departmentId}")
    public ResponseEntity<DepartmentResponse> updateDepartment(
            @PathVariable String projectId,
            @PathVariable String departmentId,
            @RequestBody UpdateDepartmentRequest request
    ) {
        return ResponseEntity.ok(departmentService.updateDepartment(projectId, departmentId, request));
    }

    @DeleteMapping("/{departmentId}")
    public ResponseEntity<MessageResponse> deleteDepartment(
            @PathVariable String projectId,
            @PathVariable String departmentId
    ) {
        return ResponseEntity.ok(departmentService.deleteDepartment(projectId, departmentId));
    }
}