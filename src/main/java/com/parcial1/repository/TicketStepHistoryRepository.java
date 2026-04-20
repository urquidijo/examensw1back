package com.parcial1.repository;

import com.parcial1.model.TicketStepHistory;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface TicketStepHistoryRepository extends MongoRepository<TicketStepHistory, String> {

    List<TicketStepHistory> findByTicketIdOrderByCompletedAtAsc(String ticketId);

    Optional<TicketStepHistory> findByIdAndProjectId(String id, String projectId);

    List<TicketStepHistory> findByProjectIdAndTicketIdOrderByCompletedAtAsc(String projectId, String ticketId);

    List<TicketStepHistory> findByProjectIdAndDepartmentIdOrderByCompletedAtDesc(String projectId, String departmentId);
}