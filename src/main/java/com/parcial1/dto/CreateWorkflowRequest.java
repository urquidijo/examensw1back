package com.parcial1.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateWorkflowRequest {

    @NotBlank(message = "El nombre del workflow es obligatorio")
    private String name;

    private String description;
}