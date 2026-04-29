package com.parcial1.dto;

import lombok.*;


import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormAiFillRequest {

    private String transcript;
    private String taskTitle;
    private String ticketTitle;
    private String ticketDescription;
    private String clientName;
    private String currentDate;

    private List<FormAiFieldDefinition> fields;
}