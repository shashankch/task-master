package com.taskmaster.ai.application.dto;

import jakarta.validation.constraints.NotBlank;

public record SuggestLabelsRequest(
    @NotBlank(message = "Task title is required")
    String title,

    String description
) {}
