package com.parcial1.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class DepartmentTaskGroupResponse {
    private String departmentId;
    private String departmentName;
    private long pendingTasksCount;
}