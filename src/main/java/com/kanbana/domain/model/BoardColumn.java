package com.kanbana.domain.model;

import java.util.UUID;

public class BoardColumn {

    private final UUID id;
    private String title;
    private int position;
    private final UUID boardId;

    public BoardColumn(UUID id, String title, int position, UUID boardId) {
        this.id = id;
        this.title = title;
        this.position = position;
        this.boardId = boardId;
    }

    public UUID getId()       { return id; }
    public String getTitle()  { return title; }
    public int getPosition()  { return position; }
    public UUID getBoardId()  { return boardId; }

    public void setTitle(String title)    { this.title = title; }
    public void setPosition(int position) { this.position = position; }
}
