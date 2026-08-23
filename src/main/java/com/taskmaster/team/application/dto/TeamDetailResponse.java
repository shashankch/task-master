package com.taskmaster.team.application.dto;

import com.taskmaster.user.application.dto.UserResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TeamDetailResponse(
    UUID id,
    String name,
    String description,
    UserResponse owner,
    String inviteCode,
    List<TeamMemberResponse> members,
    Instant createdAt,
    Instant updatedAt
) {}
