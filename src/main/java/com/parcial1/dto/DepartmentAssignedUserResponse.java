package com.parcial1.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class DepartmentAssignedUserResponse {
    private String userId;
    private String name;
    private String email;
}