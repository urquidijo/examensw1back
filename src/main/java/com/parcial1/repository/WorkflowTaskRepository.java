package com.parcial1.repository;

import com.parcial1.model.TaskStatus;
import com.parcial1.model.WorkflowTask;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface WorkflowTaskRepository extends MongoRepository<WorkflowTask, String> {

    List<WorkflowTask> findByProjectIdOrderByCreatedAtDesc(String projectId);

    List<WorkflowTask> findByAssignedUserIdAndStatusInOrderByCreatedAtDesc(
            String assignedUserId,
            List<TaskStatus> statuses
    );

    List<WorkflowTask> findByDepartmentIdOrderByCreatedAtDesc(String departmentId);
}