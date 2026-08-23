package com.taskmaster.ai.application.dto;

import java.util.List;

public record DetectDuplicatesResponse(
    List<DuplicateMatchResponse> duplicates
) {}
