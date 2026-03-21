package com.kanbana.infrastructure.persistence;

import com.kanbana.domain.model.BoardColumn;

public class BoardColumnMapper {

    private BoardColumnMapper() {}

    public static BoardColumn toDomain(BoardColumnEntity entity) {
        return new BoardColumn(
            entity.getId(),
            entity.getTitle(),
            entity.getPosition(),
            entity.getBoardId()
        );
    }

    public static BoardColumnEntity toEntity(BoardColumn column) {
        return new BoardColumnEntity(
            column.getId(),     // null on new columns (triggers INSERT), real ID on updates (triggers UPDATE)
            column.getTitle(),
            column.getPosition(),
            column.getBoardId()
        );
    }
}
