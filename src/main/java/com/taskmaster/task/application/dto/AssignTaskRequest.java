package com.taskmaster.task.application.dto;

import java.util.UUID;

public record AssignTaskRequest(
    UUID assigneeId
) {}
