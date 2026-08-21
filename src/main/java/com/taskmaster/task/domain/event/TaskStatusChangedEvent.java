package com.taskmaster.task.domain.event;

import com.taskmaster.task.domain.model.TaskStatus;
import java.time.Instant;
import java.util.UUID;

public record TaskStatusChangedEvent(
    UUID taskId,
    UUID changerId,
    UUID assigneeId,
    TaskStatus oldStatus,
    TaskStatus newStatus,
    Instant timestamp
) {
    public static TaskStatusChangedEvent of(
        UUID taskId,
        UUID changerId,
        UUID assigneeId,
        TaskStatus oldStatus,
        TaskStatus newStatus
    ) {
        return new TaskStatusChangedEvent(taskId, changerId, assigneeId, oldStatus, newStatus, Instant.now());
    }
}
