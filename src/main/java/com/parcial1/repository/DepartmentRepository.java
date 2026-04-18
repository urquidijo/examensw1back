package com.parcial1.repository;

import com.parcial1.model.Department;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends MongoRepository<Department, String> {

    List<Department> findByProjectIdOrderByCreatedAtDesc(String projectId);

    Optional<Department> findByIdAndProjectId(String id, String projectId);

    boolean existsByProjectIdAndNameIgnoreCase(String projectId, String name);
}