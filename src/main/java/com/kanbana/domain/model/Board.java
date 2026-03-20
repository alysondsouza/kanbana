package com.kanbana.domain.model;

import java.time.Instant;
import java.util.UUID;

public class Board {

    private final UUID id;
    private String title;
    private final UUID ownerId;
    private final Instant createdAt;

    public Board(UUID id, String title, UUID ownerId, Instant createdAt) {
        this.id = id;
        this.title = title;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
    }

    public UUID getId()           { return id; }
    public String getTitle()      { return title; }
    public UUID getOwnerId()      { return ownerId; }
    public Instant getCreatedAt() { return createdAt; }

    public void setTitle(String title) { this.title = title; }
}
