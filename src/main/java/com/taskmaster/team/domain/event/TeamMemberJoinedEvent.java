package com.taskmaster.team.domain.event;

import com.taskmaster.team.domain.model.TeamRole;
import java.time.Instant;
import java.util.UUID;

public record TeamMemberJoinedEvent(
    UUID teamId,
    UUID userId,
    TeamRole role,
    Instant timestamp
) {
    public static TeamMemberJoinedEvent of(UUID teamId, UUID userId, TeamRole role) {
        return new TeamMemberJoinedEvent(teamId, userId, role, Instant.now());
    }
}
