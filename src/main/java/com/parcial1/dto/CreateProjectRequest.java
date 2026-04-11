package com.parcial1.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateProjectRequest {

    @NotBlank(message = "El nombre del proyecto es obligatorio")
    private String name;

    private String description;
}