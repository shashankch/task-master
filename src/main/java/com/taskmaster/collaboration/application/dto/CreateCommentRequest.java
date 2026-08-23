package com.taskmaster.collaboration.application.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CreateCommentRequest(
    @NotBlank(message = "Comment content is required")
    String content,

    UUID parentCommentId
) {}
