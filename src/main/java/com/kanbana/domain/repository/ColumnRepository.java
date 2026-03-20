package com.kanbana.domain.repository;

import com.kanbana.domain.model.BoardColumn;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ColumnRepository {
    BoardColumn save(BoardColumn column);
    Optional<BoardColumn> findById(UUID id);
    List<BoardColumn> findByBoardId(UUID boardId);
    void deleteById(UUID id);
    boolean existsById(UUID id);
}
