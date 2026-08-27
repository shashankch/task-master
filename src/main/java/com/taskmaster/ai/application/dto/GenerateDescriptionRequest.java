package com.taskmaster.ai.application.dto;

import jakarta.validation.constraints.NotBlank;

public record GenerateDescriptionRequest(
    String title,

    @NotBlank(message = "Prompt or brief requirement is required")
    String prompt
) {}
