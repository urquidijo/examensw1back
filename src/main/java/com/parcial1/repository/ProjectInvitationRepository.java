package com.parcial1.repository;

import com.parcial1.model.InvitationStatus;
import com.parcial1.model.ProjectInvitation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectInvitationRepository extends MongoRepository<ProjectInvitation, String> {

    List<ProjectInvitation> findByProjectIdOrderByCreatedAtDesc(String projectId);

    List<ProjectInvitation> findByInvitedUserIdAndStatusOrderByCreatedAtDesc(
            String invitedUserId,
            InvitationStatus status
    );

    Optional<ProjectInvitation> findByIdAndInvitedUserId(String id, String invitedUserId);

    boolean existsByProjectIdAndInvitedUserIdAndStatus(
            String projectId,
            String invitedUserId,
            InvitationStatus status
    );
}