package com.parcial1.dto;

import com.parcial1.model.InvitationStatus;
import com.parcial1.model.ProjectRole;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ProjectInvitationResponse {
    private String id;
    private String projectId;
    private String projectName;
    private String invitedUserId;
    private String invitedName;
    private String invitedEmail;
    private ProjectRole role;
    private InvitationStatus status;
    private LocalDateTime createdAt;
}