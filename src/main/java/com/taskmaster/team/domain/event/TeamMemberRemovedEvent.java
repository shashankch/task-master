package com.taskmaster.team.domain.event;

import java.time.Instant;
import java.util.UUID;

public record TeamMemberRemovedEvent(
    UUID teamId,
    UUID userId,
    Instant timestamp
) {
    public static TeamMemberRemovedEvent of(UUID teamId, UUID userId) {
        return new TeamMemberRemovedEvent(teamId, userId, Instant.now());
    }
}
