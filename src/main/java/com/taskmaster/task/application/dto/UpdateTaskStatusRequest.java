package com.taskmaster.task.application.dto;

import com.taskmaster.task.domain.model.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTaskStatusRequest(
    @NotNull(message = "Status is required")
    TaskStatus status
) {}
