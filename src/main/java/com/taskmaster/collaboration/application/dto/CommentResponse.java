package com.taskmaster.collaboration.application.dto;

import com.taskmaster.user.application.dto.UserResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CommentResponse(
    UUID id,
    UUID taskId,
    UserResponse author,
    UUID parentCommentId,
    String content,
    boolean deleted,
    List<CommentResponse> replies,
    Instant createdAt,
    Instant updatedAt
) {}
