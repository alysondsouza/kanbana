package com.kanbana.domain.repository;

import com.kanbana.domain.model.Board;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Port — defines what the application needs from persistence.
// Infrastructure provides the implementation. No Spring imports here.
public interface BoardRepository {
    Board save(Board board);
    Optional<Board> findById(UUID id);
    List<Board> findAll();
    void deleteById(UUID id);
    boolean existsById(UUID id);
}
