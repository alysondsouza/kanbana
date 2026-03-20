package com.kanbana.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

// Pure Spring Data repository — typed to BoardEntity.
// BoardRepositoryAdapter wraps this and implements the domain BoardRepository interface.
@Repository
public interface BoardJpaRepository extends JpaRepository<BoardEntity, UUID> {
}
