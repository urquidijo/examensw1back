package com.parcial1.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class CompleteTaskRequest {
    private Map<String, Object> tramiteData;
    private String decisionResult;
}