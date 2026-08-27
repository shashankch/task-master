package com.taskmaster.ai.application.dto;

import com.taskmaster.task.domain.model.TaskPriority;

public record PriorityRecommendationResponse(
    TaskPriority suggestedPriority,
    double confidence,
    String reasoning
) {}
