package com.taskmaster.ai.application.dto;

import java.util.List;
import java.util.UUID;

public record SummarizeTaskResponse(
    UUID taskId,
    String summary,
    List<String> keyTakeaways,
    List<String> actionItems
) {}
