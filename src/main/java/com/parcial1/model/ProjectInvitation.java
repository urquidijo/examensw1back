package com.parcial1.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "project_invitations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectInvitation {

    @Id
    private String id;

    private String projectId;

    private String invitedUserId;
    private String invitedName;
    private String invitedEmail;

    private ProjectRole role;

    private String invitedByUserId;

    private InvitationStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;
}