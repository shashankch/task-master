package com.taskmaster.team.application.dto;

import com.taskmaster.team.domain.model.TeamRole;
import com.taskmaster.user.application.dto.UserResponse;
import java.time.Instant;
import java.util.UUID;

public record TeamMemberResponse(
    UUID id,
    UserResponse user,
    TeamRole role,
    Instant joinedAt
) {}
