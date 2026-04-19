package com.parcial1.repository;

import com.parcial1.model.TicketStepHistory;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TicketStepHistoryRepository extends MongoRepository<TicketStepHistory, String> {

    List<TicketStepHistory> findByTicketIdOrderByCompletedAtAsc(String ticketId);

    List<TicketStepHistory> findByProjectIdAndDepartmentIdOrderByCompletedAtDesc(String projectId, String departmentId);
}