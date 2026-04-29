package com.parcial1.repository;

import com.parcial1.model.ProjectKpiSettings;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ProjectKpiSettingsRepository extends MongoRepository<ProjectKpiSettings, String> {

    Optional<ProjectKpiSettings> findByProjectId(String projectId);
}