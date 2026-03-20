package com.kanbana.domain.model;

import java.util.UUID;

public class Card {

    private final UUID id;
    private String title;
    private String description;
    private int position;
    private UUID columnId;

    public Card(UUID id, String title, String description, int position, UUID columnId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.position = position;
        this.columnId = columnId;
    }

    public UUID getId()          { return id; }
    public String getTitle()     { return title; }
    public String getDescription(){ return description; }
    public int getPosition()     { return position; }
    public UUID getColumnId()    { return columnId; }

    public void setTitle(String title)           { this.title = title; }
    public void setDescription(String description){ this.description = description; }
    public void setPosition(int position)        { this.position = position; }
    public void setColumnId(UUID columnId)       { this.columnId = columnId; } // needed for move
}
