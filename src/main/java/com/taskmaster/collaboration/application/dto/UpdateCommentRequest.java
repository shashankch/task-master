package com.taskmaster.collaboration.application.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateCommentRequest(
    @NotBlank(message = "Comment content is required")
    String content
) {}
