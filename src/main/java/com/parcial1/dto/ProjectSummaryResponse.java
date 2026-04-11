package com.parcial1.dto;

import com.parcial1.model.ProjectRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class ProjectSummaryResponse {
    private String projectId;
    private String name;
    private String description;
    private ProjectRole role;
}