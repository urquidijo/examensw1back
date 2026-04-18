package com.parcial1.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class CreateTicketRequest {

    private String workflowId;

    private String title;
    private String description;

    private String clientName;
    private String clientPhone;
    private String clientEmail;
    private String clientReference;

    private Map<String, Object> metadata;
}