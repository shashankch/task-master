package com.taskmaster.task.application.dto;

import com.taskmaster.task.domain.model.TaskPriority;
import com.taskmaster.task.domain.model.TaskStatus;
import com.taskmaster.user.application.dto.UserResponse;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record TaskResponse(
    UUID id,
    String title,
    String description,
    TaskStatus status,
    TaskPriority priority,
    Instant dueDate,
    UserResponse creator,
    UserResponse assignee,
    UUID teamId,
    Set<String> labels,
    Long version,
    Instant createdAt,
    Instant updatedAt
) {}
