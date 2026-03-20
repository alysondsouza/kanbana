package com.kanbana.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface CardJpaRepository extends JpaRepository<CardEntity, UUID> {

    // Spring Data generates: SELECT * FROM cards WHERE column_id = ?
    List<CardEntity> findByColumnId(UUID columnId);
}
