package com.taskmaster.ai.application.dto;

import java.util.List;

public record SuggestLabelsResponse(
    List<String> suggestedLabels
) {}
