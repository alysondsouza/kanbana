package com.kanbana.domain.model;

import java.time.Instant;
import java.util.UUID;

public class User {

    private final UUID id;
    private String username;
    private String email;
    private String passwordHash;
    private final Instant createdAt;

    public User(UUID id, String username, String email, String passwordHash, Instant createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    public UUID getId()            { return id; }
    public String getUsername()    { return username; }
    public String getEmail()       { return email; }
    public String getPasswordHash(){ return passwordHash; }
    public Instant getCreatedAt()  { return createdAt; }

    public void setUsername(String username)       { this.username = username; }
    public void setEmail(String email)             { this.email = email; }
    public void setPasswordHash(String passwordHash){ this.passwordHash = passwordHash; }
}
