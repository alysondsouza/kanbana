package com.kanbana.infrastructure.persistence;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "board_columns", schema = "kanbana")
public class BoardColumnEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false)
    private int position;

    @Column(name = "board_id", nullable = false)
    private UUID boardId;

    protected BoardColumnEntity() {}

    public BoardColumnEntity(UUID id, String title, int position, UUID boardId) {
        this.id = id;
        this.title = title;
        this.position = position;
        this.boardId = boardId;
    }

    public UUID getId()      { return id; }
    public String getTitle() { return title; }
    public int getPosition() { return position; }
    public UUID getBoardId() { return boardId; }

    public void setTitle(String title)    { this.title = title; }
    public void setPosition(int position) { this.position = position; }
}
