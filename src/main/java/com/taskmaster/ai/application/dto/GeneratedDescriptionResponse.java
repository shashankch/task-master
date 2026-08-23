package com.taskmaster.ai.application.dto;

import java.util.List;

public record GeneratedDescriptionResponse(
    String description,
    List<String> suggestedAcceptanceCriteria
) {}
