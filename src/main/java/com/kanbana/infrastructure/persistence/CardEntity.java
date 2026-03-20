package com.kanbana.infrastructure.persistence;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "cards", schema = "kanbana")
public class CardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private int position;

    @Column(name = "column_id", nullable = false)
    private UUID columnId;

    protected CardEntity() {}

    public CardEntity(UUID id, String title, String description, int position, UUID columnId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.position = position;
        this.columnId = columnId;
    }

    public UUID getId()           { return id; }
    public String getTitle()      { return title; }
    public String getDescription(){ return description; }
    public int getPosition()      { return position; }
    public UUID getColumnId()     { return columnId; }

    public void setTitle(String title)            { this.title = title; }
    public void setDescription(String description){ this.description = description; }
    public void setPosition(int position)         { this.position = position; }
    public void setColumnId(UUID columnId)        { this.columnId = columnId; }
}
