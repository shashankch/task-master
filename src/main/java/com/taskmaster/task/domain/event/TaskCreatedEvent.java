package com.taskmaster.task.domain.event;

import java.time.Instant;
import java.util.UUID;

public record TaskCreatedEvent(
    UUID taskId,
    UUID creatorId,
    UUID assigneeId,
    String title,
    Instant timestamp
) {
    public static TaskCreatedEvent of(UUID taskId, UUID creatorId, UUID assigneeId, String title) {
        return new TaskCreatedEvent(taskId, creatorId, assigneeId, title, Instant.now());
    }
}
