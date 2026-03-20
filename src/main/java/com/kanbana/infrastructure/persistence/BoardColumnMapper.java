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
            null,               // let JPA generate the UUID
            column.getTitle(),
            column.getPosition(),
            column.getBoardId()
        );
    }
}
