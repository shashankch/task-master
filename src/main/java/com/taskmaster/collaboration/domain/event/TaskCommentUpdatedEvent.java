package com.taskmaster.collaboration.domain.event;

import java.time.Instant;
import java.util.UUID;

public record TaskCommentUpdatedEvent(
    UUID commentId,
    UUID taskId,
    UUID editorId,
    Instant timestamp
) {
    public static TaskCommentUpdatedEvent of(UUID commentId, UUID taskId, UUID editorId) {
        return new TaskCommentUpdatedEvent(commentId, taskId, editorId, Instant.now());
    }
}
