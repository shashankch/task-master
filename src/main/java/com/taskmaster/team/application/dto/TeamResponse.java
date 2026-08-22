package com.taskmaster.team.application.dto;

import com.taskmaster.user.application.dto.UserResponse;
import java.time.Instant;
import java.util.UUID;

public record TeamResponse(
    UUID id,
    String name,
    String description,
    UserResponse owner,
    String inviteCode,
    int memberCount,
    Instant createdAt,
    Instant updatedAt
) {}
