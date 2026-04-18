package com.parcial1.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class DepartmentResponse {
    private String id;
    private String projectId;
    private String name;
    private String description;

    private List<String> assignedUserIds;
    private List<DepartmentAssignedUserResponse> assignedUsers;

    private boolean requiresTramite;
    private String tramiteTemplateId;
    private String tramiteName;

    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}