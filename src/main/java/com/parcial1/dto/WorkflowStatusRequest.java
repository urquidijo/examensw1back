package com.parcial1.dto;

import com.parcial1.model.WorkflowStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkflowStatusRequest {

    @NotNull(message = "El estado es obligatorio")
    private WorkflowStatus status;
}