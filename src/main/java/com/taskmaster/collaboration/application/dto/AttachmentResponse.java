package com.taskmaster.collaboration.application.dto;

import com.taskmaster.user.application.dto.UserResponse;
import java.time.Instant;
import java.util.UUID;

public record AttachmentResponse(
    UUID id,
    UUID taskId,
    UserResponse uploader,
    String fileName,
    Long fileSize,
    String contentType,
    String downloadUrl,
    Instant createdAt
) {}
