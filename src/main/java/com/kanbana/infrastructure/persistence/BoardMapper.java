package com.kanbana.infrastructure.persistence;

import com.kanbana.domain.model.Board;

// Translates between domain objects and JPA entities.
// Static methods — no state, no Spring injection needed.
public class BoardMapper {

    private BoardMapper() {} // utility class — not instantiable

    public static Board toDomain(BoardEntity entity) {
        return new Board(
            entity.getId(),
            entity.getTitle(),
            entity.getOwnerId(),
            entity.getCreatedAt()
        );
    }

    public static BoardEntity toEntity(Board board) {
        return new BoardEntity(
            null,               // let JPA generate the UUID via @GeneratedValue
            board.getTitle(),
            board.getOwnerId(),
            board.getCreatedAt()
        );
    }
}
