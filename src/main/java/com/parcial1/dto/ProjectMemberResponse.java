package com.parcial1.dto;

import com.parcial1.model.ProjectRole;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ProjectMemberResponse {
    private String userId;
    private String name;
    private String email;
    private ProjectRole role;
    private String assignedNodeId;
}