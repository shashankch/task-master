package com.taskmaster.collaboration.domain.event;

import java.time.Instant;
import java.util.UUID;

public record TaskCommentDeletedEvent(
    UUID commentId,
    UUID taskId,
    UUID deleterId,
    Instant timestamp
) {
    public static TaskCommentDeletedEvent of(UUID commentId, UUID taskId, UUID deleterId) {
        return new TaskCommentDeletedEvent(commentId, taskId, deleterId, Instant.now());
    }
}
