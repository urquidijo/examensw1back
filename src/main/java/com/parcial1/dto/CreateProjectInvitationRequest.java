package com.parcial1.dto;

import com.parcial1.model.ProjectRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateProjectInvitationRequest {

    private String email;
    private String name;

    @NotNull(message = "El rol es obligatorio")
    private ProjectRole role;
}