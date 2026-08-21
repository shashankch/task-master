package com.taskmaster.task.domain.event;

import java.time.Instant;
import java.util.UUID;

public record TaskDeletedEvent(
    UUID taskId,
    UUID deleterId,
    Instant timestamp
) {
    public static TaskDeletedEvent of(UUID taskId, UUID deleterId) {
        return new TaskDeletedEvent(taskId, deleterId, Instant.now());
    }
}
