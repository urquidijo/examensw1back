package com.parcial1.repository;

import com.parcial1.model.TramiteTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface TramiteTemplateRepository extends MongoRepository<TramiteTemplate, String> {

    List<TramiteTemplate> findByProjectIdOrderByCreatedAtDesc(String projectId);

    Optional<TramiteTemplate> findByIdAndProjectId(String id, String projectId);

    boolean existsByProjectIdAndNameIgnoreCase(String projectId, String name);
}