package com.taskmaster.user.application.dto;

import com.taskmaster.user.domain.model.Role;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String email,
    String username,
    String displayName,
    String avatarUrl,
    Role role,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {}
