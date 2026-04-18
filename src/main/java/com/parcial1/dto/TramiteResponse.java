package com.parcial1.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
public class TramiteResponse {
    private String id;
    private String projectId;
    private String name;
    private String description;
    private boolean active;
    private int fieldsCount;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<Map<String, Object>> fields;
}