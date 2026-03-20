package com.kanbana.application.dto;

import com.kanbana.domain.model.Board;
import java.time.Instant;
import java.util.UUID;

public class BoardResponseDTO {

    private UUID id;
    private String title;
    private UUID ownerId;
    private Instant createdAt;

    public BoardResponseDTO() {}

    public static BoardResponseDTO from(Board board) {
        BoardResponseDTO response = new BoardResponseDTO();
        response.id = board.getId();
        response.title = board.getTitle();
        response.ownerId = board.getOwnerId();
        response.createdAt = board.getCreatedAt();
        return response;
    }

    public UUID getId()           { return id; }
    public String getTitle()      { return title; }
    public UUID getOwnerId()      { return ownerId; }
    public Instant getCreatedAt() { return createdAt; }
}
