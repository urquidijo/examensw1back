package com.parcial1.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class CreateTramiteRequest {

    @NotBlank(message = "El nombre del trámite es obligatorio")
    private String name;

    private String description;

    private Boolean active;

    private List<Map<String, Object>> fields;
}