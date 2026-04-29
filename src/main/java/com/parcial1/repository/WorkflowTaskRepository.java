package com.parcial1.repository;

import com.parcial1.model.TaskStatus;
import com.parcial1.model.WorkflowTask;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface WorkflowTaskRepository extends MongoRepository<WorkflowTask, String> {

        List<WorkflowTask> findByProjectIdOrderByCreatedAtDesc(String projectId);

        List<WorkflowTask> findByAssignedUserIdAndStatusInOrderByCreatedAtDesc(
                        String assignedUserId,
                        List<TaskStatus> statuses);

        List<WorkflowTask> findByDepartmentIdOrderByCreatedAtDesc(String departmentId);

        List<WorkflowTask> findByProjectIdAndAssignedUserIdAndDepartmentIdAndStatusInOrderByCreatedAtDesc(
                        String projectId,
                        String assignedUserId,
                        String departmentId,
                        List<TaskStatus> statuses);

        List<WorkflowTask> findByProjectIdAndTicketIdAndStatusInOrderByCreatedAtAsc(
                        String projectId,
                        String ticketId,
                        List<TaskStatus> statuses);

        Optional<WorkflowTask> findByIdAndProjectId(String id, String projectId);

        List<WorkflowTask> findByProjectIdAndStatusInOrderByCreatedAtDesc(
                        String projectId,
                        List<TaskStatus> statuses);
}