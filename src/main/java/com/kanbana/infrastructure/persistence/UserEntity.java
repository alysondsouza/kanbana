package com.kanbana.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users", schema = "kanbana")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // JPA requires a no-arg constructor
    protected UserEntity() {}

    public UserEntity(UUID id, String username, String email, String passwordHash, Instant createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    public UUID getId()             { return id; }
    public String getUsername()     { return username; }
    public String getEmail()        { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Instant getCreatedAt()   { return createdAt; }

    public void setUsername(String username)        { this.username = username; }
    public void setEmail(String email)              { this.email = email; }
    public void setPasswordHash(String passwordHash){ this.passwordHash = passwordHash; }
}
