package com.kanbana.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ColumnJpaRepository extends JpaRepository<BoardColumnEntity, UUID> {

    // Spring Data generates this query from the method name automatically:
    // SELECT * FROM board_columns WHERE board_id = ?
    List<BoardColumnEntity> findByBoardId(UUID boardId);
}
