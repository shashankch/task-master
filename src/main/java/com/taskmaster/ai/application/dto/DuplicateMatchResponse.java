package com.taskmaster.ai.application.dto;

import java.util.UUID;

public record DuplicateMatchResponse(
    UUID taskId,
    String title,
    double similarityScore,
    String reason
) {}
