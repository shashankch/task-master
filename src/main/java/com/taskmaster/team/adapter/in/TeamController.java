package com.taskmaster.team.adapter.in;

import com.taskmaster.shared.dto.ApiResponse;
import com.taskmaster.team.application.dto.CreateTeamRequest;
import com.taskmaster.team.application.dto.JoinTeamRequest;
import com.taskmaster.team.application.dto.TeamDetailResponse;
import com.taskmaster.team.application.dto.TeamMemberResponse;
import com.taskmaster.team.application.dto.TeamResponse;
import com.taskmaster.team.application.dto.UpdateMemberRoleRequest;
import com.taskmaster.team.application.dto.UpdateTeamRequest;
import com.taskmaster.team.application.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teams")
@Tag(name = "Teams", description = "Team workspace creation, membership governance, invite codes, and role management")
@SecurityRequirement(name = "BearerAuth")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @PostMapping
    @Operation(summary = "Create a new team workspace")
    public ResponseEntity<ApiResponse<TeamResponse>> createTeam(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody CreateTeamRequest request
    ) {
        UUID ownerId = UUID.fromString(jwt.getSubject());
        TeamResponse response = teamService.createTeam(ownerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get team workspace details by ID")
    public ResponseEntity<ApiResponse<TeamDetailResponse>> getTeamById(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("id") UUID id
    ) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        TeamDetailResponse response = teamService.getTeamById(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @GetMapping
    @Operation(summary = "List all team workspaces the current user belongs to")
    public ResponseEntity<ApiResponse<List<TeamResponse>>> getUserTeams(@AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        List<TeamResponse> response = teamService.getUserTeams(currentUserId);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update team workspace name or description (OWNER/ADMIN only)")
    public ResponseEntity<ApiResponse<TeamResponse>> updateTeam(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("id") UUID id,
        @Valid @RequestBody UpdateTeamRequest request
    ) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        TeamResponse response = teamService.updateTeam(id, currentUserId, request);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PostMapping("/join")
    @Operation(summary = "Join a team workspace using an invite code")
    public ResponseEntity<ApiResponse<TeamResponse>> joinTeam(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody JoinTeamRequest request
    ) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        TeamResponse response = teamService.joinTeamByInviteCode(currentUserId, request);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PostMapping("/{id}/invite-code/regenerate")
    @Operation(summary = "Regenerate team invite code (OWNER/ADMIN only)")
    public ResponseEntity<ApiResponse<TeamResponse>> regenerateInviteCode(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("id") UUID id
    ) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        TeamResponse response = teamService.regenerateInviteCode(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @GetMapping("/{id}/members")
    @Operation(summary = "List all members of a team workspace")
    public ResponseEntity<ApiResponse<List<TeamMemberResponse>>> getTeamMembers(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("id") UUID id
    ) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        List<TeamMemberResponse> response = teamService.getTeamMembers(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PatchMapping("/{id}/members/{userId}/role")
    @Operation(summary = "Update member role in team workspace (OWNER only)")
    public ResponseEntity<ApiResponse<TeamMemberResponse>> updateMemberRole(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("id") UUID teamId,
        @PathVariable("userId") UUID targetUserId,
        @Valid @RequestBody UpdateMemberRoleRequest request
    ) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        TeamMemberResponse response = teamService.updateMemberRole(teamId, targetUserId, currentUserId, request);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @DeleteMapping("/{id}/members/{userId}")
    @Operation(summary = "Remove a member from team workspace or leave team")
    public ResponseEntity<ApiResponse<Map<String, String>>> removeMember(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("id") UUID teamId,
        @PathVariable("userId") UUID targetUserId
    ) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        teamService.removeMember(teamId, targetUserId, currentUserId);
        return ResponseEntity.ok(ApiResponse.of(Map.of("message", "Member successfully removed")));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete team workspace (OWNER only)")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteTeam(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("id") UUID id
    ) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        teamService.deleteTeam(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.of(Map.of("message", "Team successfully deleted")));
    }
}
