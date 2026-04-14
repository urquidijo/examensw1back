package com.parcial1.repository;

import com.parcial1.model.ProjectMember;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends MongoRepository<ProjectMember, String> {

    List<ProjectMember> findByUserId(String userId);

    List<ProjectMember> findByProjectId(String projectId);

    Optional<ProjectMember> findByProjectIdAndUserId(String projectId, String userId);

    List<ProjectMember> findByProjectIdAndAssignedNodeId(String projectId, String assignedNodeId);

    boolean existsByProjectIdAndUserId(String projectId, String userId);

    void deleteByProjectId(String projectId);

    long countByProjectId(String projectId);
}