package com.taskmaster.task.application.dto;

import com.taskmaster.task.domain.model.TaskPriority;
import com.taskmaster.task.domain.model.TaskStatus;
import java.time.Instant;
import java.util.List;
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
    Boolean includeDeleted,
    UUID currentUserId,
    List<UUID> allowedTeamIds
) {
    public TaskFilterCriteria(
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
    ) {
        this(status, priority, assigneeId, creatorId, teamId, search, dueDateFrom, dueDateTo, label, includeDeleted, null, null);
    }
}
