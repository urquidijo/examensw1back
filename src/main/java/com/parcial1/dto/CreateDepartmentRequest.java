package com.parcial1.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateDepartmentRequest {

    @NotBlank(message = "El nombre del departamento es obligatorio")
    private String name;

    private String description;

    private List<String> assignedUserIds;

    private Boolean requiresTramite;

    private String tramiteTemplateId;
}