package com.kanbana.application.dto;

import com.kanbana.domain.model.BoardColumn;
import java.util.UUID;

public class ColumnResponseDTO {

    private UUID id;
    private String title;
    private int position;
    private UUID boardId;

    public ColumnResponseDTO() {}

    public static ColumnResponseDTO from(BoardColumn column) {
        ColumnResponseDTO response = new ColumnResponseDTO();
        response.id = column.getId();
        response.title = column.getTitle();
        response.position = column.getPosition();
        response.boardId = column.getBoardId();
        return response;
    }

    public UUID getId()      { return id; }
    public String getTitle() { return title; }
    public int getPosition() { return position; }
    public UUID getBoardId() { return boardId; }
}
