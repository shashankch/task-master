package com.taskmaster.task.domain.event;

import java.time.Instant;
import java.util.UUID;

public record TaskUpdatedEvent(
    UUID taskId,
    UUID updaterId,
    String title,
    Instant timestamp
) {
    public static TaskUpdatedEvent of(UUID taskId, UUID updaterId, String title) {
        return new TaskUpdatedEvent(taskId, updaterId, title, Instant.now());
    }
}
