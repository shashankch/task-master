package com.taskmaster.team.domain.event;

import com.taskmaster.team.domain.model.TeamRole;
import java.time.Instant;
import java.util.UUID;

public record TeamMemberRoleUpdatedEvent(
    UUID teamId,
    UUID userId,
    TeamRole oldRole,
    TeamRole newRole,
    Instant timestamp
) {
    public static TeamMemberRoleUpdatedEvent of(UUID teamId, UUID userId, TeamRole oldRole, TeamRole newRole) {
        return new TeamMemberRoleUpdatedEvent(teamId, userId, oldRole, newRole, Instant.now());
    }
}
