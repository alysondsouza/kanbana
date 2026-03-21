package com.kanbana.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    // Spring Data generates: SELECT * FROM users WHERE username = ?
    Optional<UserEntity> findByUsername(String username);

    // Spring Data generates: SELECT COUNT(*) > 0 FROM users WHERE username = ?
    boolean existsByUsername(String username);

    // Spring Data generates: SELECT COUNT(*) > 0 FROM users WHERE email = ?
    boolean existsByEmail(String email);
}
