package com.kanbana.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "boards", schema = "kanbana")
public class BoardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String title;

    // Stores the FK — no @ManyToOne to User, keeping it simple (no auth yet)
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected BoardEntity() {}

    public BoardEntity(UUID id, String title, UUID ownerId, Instant createdAt) {
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
