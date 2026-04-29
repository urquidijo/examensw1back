package com.parcial1.dto;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormAiFillResponse {

    private String summary;
    private Map<String, Object> values;
}