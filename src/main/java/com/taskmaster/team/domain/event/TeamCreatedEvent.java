package com.taskmaster.team.domain.event;

import java.time.Instant;
import java.util.UUID;

public record TeamCreatedEvent(
    UUID teamId,
    UUID ownerId,
    String name,
    Instant timestamp
) {
    public static TeamCreatedEvent of(UUID teamId, UUID ownerId, String name) {
        return new TeamCreatedEvent(teamId, ownerId, name, Instant.now());
    }
}
