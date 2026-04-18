package com.parcial1.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "departments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department {

    @Id
    private String id;

    private String projectId;
    private String name;
    private String description;

    private List<String> assignedUserIds;

    private boolean requiresTramite;
    private String tramiteTemplateId;

    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}