package com.taskmaster.ai.application.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record DetectDuplicatesRequest(
    @NotBlank(message = "Task title is required")
    String title,

    String description,

    UUID teamId
) {}
