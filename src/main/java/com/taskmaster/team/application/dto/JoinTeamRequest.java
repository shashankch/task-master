package com.taskmaster.team.application.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinTeamRequest(
    @NotBlank(message = "Invite code is required")
    String inviteCode
) {}
