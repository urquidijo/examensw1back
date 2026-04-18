package com.parcial1.repository;

import com.parcial1.model.Workflow;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface WorkflowRepository extends MongoRepository<Workflow, String> {

    List<Workflow> findByProjectIdOrderByCreatedAtDesc(String projectId);

    Optional<Workflow> findByIdAndProjectId(String id, String projectId);

    long countByProjectId(String projectId);

    void deleteByIdAndProjectId(String id, String projectId);
}