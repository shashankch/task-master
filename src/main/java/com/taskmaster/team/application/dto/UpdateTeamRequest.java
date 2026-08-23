package com.taskmaster.team.application.dto;

import jakarta.validation.constraints.Size;

public record UpdateTeamRequest(
    @Size(min = 1, max = 100, message = "Team name must be between 1 and 100 characters")
    String name,

    String description
) {}
