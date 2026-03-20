package com.kanbana.infrastructure.persistence;

import com.kanbana.domain.model.BoardColumn;
import com.kanbana.domain.repository.ColumnRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class ColumnRepositoryAdapter implements ColumnRepository {

    private final ColumnJpaRepository jpa;

    public ColumnRepositoryAdapter(ColumnJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public BoardColumn save(BoardColumn column) {
        BoardColumnEntity entity = BoardColumnMapper.toEntity(column);
        BoardColumnEntity saved = jpa.save(entity);
        return BoardColumnMapper.toDomain(saved);
    }

    @Override
    public Optional<BoardColumn> findById(UUID id) {
        return jpa.findById(id).map(BoardColumnMapper::toDomain);
    }

    @Override
    public List<BoardColumn> findByBoardId(UUID boardId) {
        return jpa.findByBoardId(boardId)
                  .stream()
                  .map(BoardColumnMapper::toDomain)
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
