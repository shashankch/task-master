package com.taskmaster.user.application.mapper;

import com.taskmaster.user.application.dto.RegisterRequest;
import com.taskmaster.user.application.dto.UserResponse;
import com.taskmaster.user.domain.model.Role;
import com.taskmaster.user.domain.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(RegisterRequest request, String passwordHash) {
        if (request == null) {
            return null;
        }
        return new User(
            request.email().trim().toLowerCase(),
            request.username().trim(),
            passwordHash,
            request.displayName() != null ? request.displayName().trim() : null,
            Role.USER
        );
    }

    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getUsername(),
            user.getDisplayName(),
            user.getAvatarUrl(),
            user.getRole(),
            user.isActive(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
}
