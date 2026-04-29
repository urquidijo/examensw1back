package com.parcial1.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectAssistantChatResponse {

    private String answer;
    private List<ProjectAssistantActionDto> actions;
    private List<String> suggestions;
}