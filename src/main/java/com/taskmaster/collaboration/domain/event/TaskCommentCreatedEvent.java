package com.taskmaster.collaboration.domain.event;

import java.time.Instant;
import java.util.UUID;

public record TaskCommentCreatedEvent(
    UUID commentId,
    UUID taskId,
    UUID authorId,
    UUID parentCommentId,
    Instant timestamp
) {
    public static TaskCommentCreatedEvent of(UUID commentId, UUID taskId, UUID authorId, UUID parentCommentId) {
        return new TaskCommentCreatedEvent(commentId, taskId, authorId, parentCommentId, Instant.now());
    }
}
