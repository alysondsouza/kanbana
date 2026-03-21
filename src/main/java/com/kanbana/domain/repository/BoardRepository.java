package com.kanbana.domain.repository;

import com.kanbana.domain.model.Board;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BoardRepository {
    Board save(Board board);
    Optional<Board> findById(UUID id);
    List<Board> findAll();
    List<Board> findByOwnerId(UUID ownerId);    // returns only boards belonging to this user
    void deleteById(UUID id);
    boolean existsById(UUID id);
}
