package com.parcial1.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectAssistantChatRequest {

    private String message;
    private String projectName;
    private String currentModule;

    private List<ProjectAssistantMessageDto> history;
}