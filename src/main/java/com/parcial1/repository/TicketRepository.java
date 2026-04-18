package com.parcial1.repository;

import com.parcial1.model.Ticket;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends MongoRepository<Ticket, String> {

    List<Ticket> findByProjectIdOrderByCreatedAtDesc(String projectId);

    Optional<Ticket> findByIdAndProjectId(String id, String projectId);

    long countByWorkflowId(String workflowId);
}