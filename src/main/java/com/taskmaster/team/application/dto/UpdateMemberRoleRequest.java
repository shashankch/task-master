package com.taskmaster.team.application.dto;

import com.taskmaster.team.domain.model.TeamRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(
    @NotNull(message = "Role is required")
    TeamRole role
) {}
