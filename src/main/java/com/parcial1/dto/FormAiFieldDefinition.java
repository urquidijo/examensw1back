package com.parcial1.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormAiFieldDefinition {

    private String id;
    private String label;
    private String type;
    private Boolean required;
    private String placeholder;
    private List<String> options;
}