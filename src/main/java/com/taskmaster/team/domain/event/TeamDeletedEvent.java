package com.taskmaster.team.domain.event;

import java.time.Instant;
import java.util.UUID;

public record TeamDeletedEvent(
    UUID teamId,
    UUID deleterId,
    Instant timestamp
) {
    public static TeamDeletedEvent of(UUID teamId, UUID deleterId) {
        return new TeamDeletedEvent(teamId, deleterId, Instant.now());
    }
}
