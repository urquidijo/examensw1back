package com.parcial1.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NodeInviteDetailsResponse {
    private String projectId;
    private String projectName;
    private String nodeId;
    private String nodeLabel;
    private LocalDateTime expiresAt;
}