package com.kanbana.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BoardJpaRepository extends JpaRepository<BoardEntity, UUID> {

    // Spring Data generates: SELECT * FROM boards WHERE owner_id = ?
    List<BoardEntity> findByOwnerId(UUID ownerId);
}
