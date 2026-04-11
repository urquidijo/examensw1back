package com.parcial1.repository;

import com.parcial1.model.WorkflowDiagram;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface WorkflowDiagramRepository extends MongoRepository<WorkflowDiagram, String> {
    Optional<WorkflowDiagram> findByProjectId(String projectId);
}