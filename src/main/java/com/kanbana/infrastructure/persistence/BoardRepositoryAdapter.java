package com.kanbana.infrastructure.persistence;

import com.kanbana.domain.model.Board;
import com.kanbana.domain.repository.BoardRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class BoardRepositoryAdapter implements BoardRepository {

    private final BoardJpaRepository jpa;

    public BoardRepositoryAdapter(BoardJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Board save(Board board) {
        BoardEntity entity = BoardMapper.toEntity(board);
        BoardEntity saved = jpa.save(entity);
        return BoardMapper.toDomain(saved);
    }

    @Override
    public Optional<Board> findById(UUID id) {
        return jpa.findById(id).map(BoardMapper::toDomain);
    }

    @Override
    public List<Board> findAll() {
        return jpa.findAll()
                  .stream()
                  .map(BoardMapper::toDomain)
                  .collect(Collectors.toList());
    }

    @Override
    public List<Board> findByOwnerId(UUID ownerId) {
        return jpa.findByOwnerId(ownerId)
                  .stream()
                  .map(BoardMapper::toDomain)
                  .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        jpa.deleteById(id);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpa.existsById(id);
    }
}
