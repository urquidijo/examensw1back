package com.parcial1.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class DepartmentTaskBoardResponse {
    private String departmentId;
    private String departmentName;
    private long activeTasksCount;
    private long completedTasksCount;
    private boolean assignedToMe;
}