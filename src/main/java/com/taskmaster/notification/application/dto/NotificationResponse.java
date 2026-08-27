package com.taskmaster.notification.application.dto;

import com.taskmaster.notification.domain.model.NotificationType;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
    UUID id,
    UUID recipientId,
    NotificationType type,
    String title,
    String message,
    String metadata,
    boolean isRead,
    Instant createdAt,
    Instant readAt
) {}
