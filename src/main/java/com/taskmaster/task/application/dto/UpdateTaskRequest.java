package com.taskmaster.task.application.dto;

import com.taskmaster.task.domain.model.TaskPriority;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;

public record UpdateTaskRequest(
    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    String title,

    String description,

    TaskPriority priority,

    Instant dueDate,

    Set<@Size(max = 50, message = "Label must not exceed 50 characters") String> labels
) {}
