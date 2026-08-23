package com.taskmaster.collaboration.domain.event;

import java.time.Instant;
import java.util.UUID;

public record TaskAttachmentUploadedEvent(
    UUID attachmentId,
    UUID taskId,
    UUID uploaderId,
    String fileName,
    Instant timestamp
) {
    public static TaskAttachmentUploadedEvent of(UUID attachmentId, UUID taskId, UUID uploaderId, String fileName) {
        return new TaskAttachmentUploadedEvent(attachmentId, taskId, uploaderId, fileName, Instant.now());
    }
}
