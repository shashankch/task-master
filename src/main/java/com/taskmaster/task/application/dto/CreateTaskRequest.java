package com.taskmaster.task.application.dto;

import com.taskmaster.task.domain.model.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record CreateTaskRequest(
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    String title,

    String description,

    TaskPriority priority,

    Instant dueDate,

    UUID assigneeId,

    UUID teamId,

    Set<@Size(max = 50, message = "Label must not exceed 50 characters") String> labels
) {}
