package com.parcial1.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "node_invites")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NodeInvite {

    @Id
    private String id;

    private String token;
    private String projectId;
    private String nodeId;
    private String nodeLabel;

    private String createdByUserId;
    private String acceptedByUserId;

    private boolean accepted;
    private boolean revoked;

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime acceptedAt;
}