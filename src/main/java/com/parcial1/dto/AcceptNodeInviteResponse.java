package com.parcial1.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcceptNodeInviteResponse {
    private String message;
    private String projectId;
    private String nodeId;
}