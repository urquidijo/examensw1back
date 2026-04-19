package com.parcial1.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "parallel_join_states")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParallelJoinState {

    @Id
    private String id;

    private String projectId;
    private String ticketId;
    private String workflowId;

    private String parallelGroupId;
    private String forkNodeId;
    private String joinNodeId;

    private int expectedBranches;
    private List<String> arrivedBranchSourceNodeIds;

    private boolean released;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime releasedAt;
}