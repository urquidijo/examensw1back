package com.parcial1.repository;

import com.parcial1.model.NodeInvite;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface NodeInviteRepository extends MongoRepository<NodeInvite, String> {

    Optional<NodeInvite> findByToken(String token);

    boolean existsByToken(String token);
}