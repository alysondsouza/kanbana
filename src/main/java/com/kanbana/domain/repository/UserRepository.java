package com.kanbana.domain.repository;

import com.kanbana.domain.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(UUID id);
    Optional<User> findByUsername(String username);  // needed for login
    boolean existsByUsername(String username);        // needed for register uniqueness check
    boolean existsByEmail(String email);             // needed for register uniqueness check
}
