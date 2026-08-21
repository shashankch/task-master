package com.taskmaster.task.application.dto;

import com.taskmaster.task.domain.model.TaskPriority;
import com.taskmaster.task.domain.model.TaskStatus;
import java.time.Instant;
import java.util.UUID;

public record TaskFilterCriteria(
    TaskStatus status,
    TaskPriority priority,
    UUID assigneeId,
    UUID creatorId,
    UUID teamId,
    String search,
    Instant dueDateFrom,
    Instant dueDateTo,
    String label,
    Boolean includeDeleted
) {}
