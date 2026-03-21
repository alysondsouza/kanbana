package com.kanbana.infrastructure.persistence;

import com.kanbana.domain.model.User;
import com.kanbana.domain.repository.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpa;

    public UserRepositoryAdapter(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public User save(User user) {
        UserEntity entity = UserMapper.toEntity(user);
        UserEntity saved = jpa.save(entity);
        return UserMapper.toDomain(saved);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpa.findById(id).map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpa.findByUsername(username).map(UserMapper::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpa.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpa.existsByEmail(email);
    }
}
