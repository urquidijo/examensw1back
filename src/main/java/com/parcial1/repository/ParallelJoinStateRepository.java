package com.parcial1.repository;

import com.parcial1.model.ParallelJoinState;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ParallelJoinStateRepository extends MongoRepository<ParallelJoinState, String> {

    Optional<ParallelJoinState> findByTicketIdAndJoinNodeIdAndParallelGroupId(
            String ticketId,
            String joinNodeId,
            String parallelGroupId
    );
}