package com.taskmaster.task.domain.event;

import java.time.Instant;
import java.util.UUID;

public record TaskAssignedEvent(
    UUID taskId,
    UUID assignerId,
    UUID assigneeId,
    String title,
    Instant timestamp
) {
    public static TaskAssignedEvent of(UUID taskId, UUID assignerId, UUID assigneeId, String title) {
        return new TaskAssignedEvent(taskId, assignerId, assigneeId, title, Instant.now());
    }
}
