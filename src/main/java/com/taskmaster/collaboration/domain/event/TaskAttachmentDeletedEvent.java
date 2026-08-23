package com.taskmaster.collaboration.domain.event;

import java.time.Instant;
import java.util.UUID;

public record TaskAttachmentDeletedEvent(
    UUID attachmentId,
    UUID taskId,
    UUID deleterId,
    Instant timestamp
) {
    public static TaskAttachmentDeletedEvent of(UUID attachmentId, UUID taskId, UUID deleterId) {
        return new TaskAttachmentDeletedEvent(attachmentId, taskId, deleterId, Instant.now());
    }
}
