package com.parcial1.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = "tramite_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TramiteTemplate {

    @Id
    private String id;

    private String projectId;
    private String name;
    private String description;
    private boolean active;

    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<Map<String, Object>> fields;
}