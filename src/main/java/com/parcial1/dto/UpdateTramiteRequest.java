package com.parcial1.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class UpdateTramiteRequest {

    private String name;
    private String description;
    private Boolean active;
    private List<Map<String, Object>> fields;
}