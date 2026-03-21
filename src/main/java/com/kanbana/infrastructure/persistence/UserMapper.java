package com.kanbana.infrastructure.persistence;

import com.kanbana.domain.model.User;

public class UserMapper {

    private UserMapper() {}

    public static User toDomain(UserEntity entity) {
        return new User(
                entity.getId(),
                entity.getUsername(),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getCreatedAt()
        );
    }

    public static UserEntity toEntity(User user) {
        return new UserEntity(
                null,               // let JPA generate UUID on INSERT — users are never updated via this path
                user.getUsername(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getCreatedAt()
        );
    }
}
