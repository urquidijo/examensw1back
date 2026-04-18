package com.parcial1.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateDepartmentRequest {

    private String name;
    private String description;
    private List<String> assignedUserIds;
    private Boolean requiresTramite;
    private String tramiteTemplateId;
}