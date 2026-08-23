package com.taskmaster.team.application.service;

import com.taskmaster.shared.exception.BadRequestException;
import com.taskmaster.shared.exception.ConflictException;
import com.taskmaster.shared.exception.ForbiddenException;
import com.taskmaster.shared.exception.ResourceNotFoundException;
import com.taskmaster.team.application.dto.CreateTeamRequest;
import com.taskmaster.team.application.dto.JoinTeamRequest;
import com.taskmaster.team.application.dto.TeamDetailResponse;
import com.taskmaster.team.application.dto.TeamMemberResponse;
import com.taskmaster.team.application.dto.TeamResponse;
import com.taskmaster.team.application.dto.UpdateMemberRoleRequest;
import com.taskmaster.team.application.dto.UpdateTeamRequest;
import com.taskmaster.team.application.mapper.TeamMapper;
import com.taskmaster.team.domain.event.TeamCreatedEvent;
import com.taskmaster.team.domain.event.TeamDeletedEvent;
import com.taskmaster.team.domain.event.TeamMemberJoinedEvent;
import com.taskmaster.team.domain.event.TeamMemberRemovedEvent;
import com.taskmaster.team.domain.event.TeamMemberRoleUpdatedEvent;
import com.taskmaster.team.domain.model.Team;
import com.taskmaster.team.domain.model.TeamMember;
import com.taskmaster.team.domain.model.TeamRole;
import com.taskmaster.team.domain.port.TeamEventPublisher;
import com.taskmaster.team.domain.port.TeamMemberRepository;
import com.taskmaster.team.domain.port.TeamRepository;
import com.taskmaster.user.domain.model.User;
import com.taskmaster.user.domain.port.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service managing Team workspaces, membership lifecycle, roles, and invite codes.
 */
@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final TeamMapper teamMapper;
    private final TeamEventPublisher teamEventPublisher;

    public TeamService(
        TeamRepository teamRepository,
        TeamMemberRepository teamMemberRepository,
        UserRepository userRepository,
        TeamMapper teamMapper,
        TeamEventPublisher teamEventPublisher
    ) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
        this.teamMapper = teamMapper;
        this.teamEventPublisher = teamEventPublisher;
    }

    @Transactional
    public TeamResponse createTeam(UUID ownerId, CreateTeamRequest request) {
        User owner = userRepository.findById(ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", ownerId));

        Team team = new Team(request.name().trim(), request.description(), owner);
        team.addMember(owner, TeamRole.OWNER);

        Team savedTeam = teamRepository.save(team);

        teamEventPublisher.publish(TeamCreatedEvent.of(savedTeam.getId(), owner.getId(), savedTeam.getName()));
        teamEventPublisher.publish(TeamMemberJoinedEvent.of(savedTeam.getId(), owner.getId(), TeamRole.OWNER));

        return teamMapper.toResponse(savedTeam);
    }

    @Transactional(readOnly = true)
    public TeamDetailResponse getTeamById(UUID teamId, UUID currentUserId) {
        TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId)
            .orElseThrow(() -> new ForbiddenException("You are not a member of this team"));

        Team team = teamRepository.findById(teamId)
            .orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));

        return teamMapper.toDetailResponse(team);
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> getUserTeams(UUID userId) {
        List<Team> teams = teamRepository.findAllByUserId(userId);
        return teams.stream().map(teamMapper::toResponse).toList();
    }

    @Transactional
    public TeamResponse updateTeam(UUID teamId, UUID currentUserId, UpdateTeamRequest request) {
        TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId)
            .orElseThrow(() -> new ForbiddenException("You are not a member of this team"));

        if (member.getRole() != TeamRole.OWNER && member.getRole() != TeamRole.ADMIN) {
            throw new ForbiddenException("Only team owners and administrators can update team details");
        }

        Team team = teamRepository.findById(teamId)
            .orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));

        if (request.name() != null && !request.name().isBlank()) {
            team.setName(request.name().trim());
        }
        if (request.description() != null) {
            team.setDescription(request.description().trim());
        }

        Team updatedTeam = teamRepository.save(team);
        return teamMapper.toResponse(updatedTeam);
    }

    @Transactional
    public TeamResponse joinTeamByInviteCode(UUID userId, JoinTeamRequest request) {
        Team team = teamRepository.findByInviteCode(request.inviteCode().trim())
            .orElseThrow(() -> new ResourceNotFoundException("Team", "inviteCode", request.inviteCode()));

        if (teamMemberRepository.existsByTeamIdAndUserId(team.getId(), userId)) {
            throw new ConflictException("You are already a member of this team");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        TeamMember newMember = new TeamMember(team, user, TeamRole.MEMBER);
        team.getMembers().add(newMember);
        teamMemberRepository.save(newMember);

        teamEventPublisher.publish(TeamMemberJoinedEvent.of(team.getId(), user.getId(), TeamRole.MEMBER));

        return teamMapper.toResponse(team);
    }

    @Transactional
    public TeamResponse regenerateInviteCode(UUID teamId, UUID currentUserId) {
        TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId)
            .orElseThrow(() -> new ForbiddenException("You are not a member of this team"));

        if (member.getRole() != TeamRole.OWNER && member.getRole() != TeamRole.ADMIN) {
            throw new ForbiddenException("Only team owners and administrators can regenerate invite codes");
        }

        Team team = teamRepository.findById(teamId)
            .orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));

        team.regenerateInviteCode();
        Team updatedTeam = teamRepository.save(team);
        return teamMapper.toResponse(updatedTeam);
    }

    @Transactional(readOnly = true)
    public List<TeamMemberResponse> getTeamMembers(UUID teamId, UUID currentUserId) {
        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, currentUserId)) {
            throw new ForbiddenException("You are not a member of this team");
        }

        List<TeamMember> members = teamMemberRepository.findAllByTeamId(teamId);
        return members.stream().map(teamMapper::toMemberResponse).toList();
    }

    @Transactional
    public TeamMemberResponse updateMemberRole(
        UUID teamId,
        UUID targetUserId,
        UUID currentUserId,
        UpdateMemberRoleRequest request
    ) {
        TeamMember currentMember = teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId)
            .orElseThrow(() -> new ForbiddenException("You are not a member of this team"));

        if (currentMember.getRole() != TeamRole.OWNER) {
            throw new ForbiddenException("Only the team owner can modify member roles");
        }

        if (targetUserId.equals(currentUserId)) {
            throw new BadRequestException("Owner role cannot be modified");
        }

        if (request.role() == TeamRole.OWNER) {
            throw new BadRequestException("Cannot assign OWNER role via role update");
        }

        TeamMember targetMember = teamMemberRepository.findByTeamIdAndUserId(teamId, targetUserId)
            .orElseThrow(() -> new ResourceNotFoundException("TeamMember", "userId", targetUserId));

        TeamRole oldRole = targetMember.getRole();
        targetMember.setRole(request.role());
        TeamMember updated = teamMemberRepository.save(targetMember);

        teamEventPublisher.publish(TeamMemberRoleUpdatedEvent.of(teamId, targetUserId, oldRole, request.role()));

        return teamMapper.toMemberResponse(updated);
    }

    @Transactional
    public void removeMember(UUID teamId, UUID targetUserId, UUID currentUserId) {
        TeamMember currentMember = teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId)
            .orElseThrow(() -> new ForbiddenException("You are not a member of this team"));

        TeamMember targetMember = teamMemberRepository.findByTeamIdAndUserId(teamId, targetUserId)
            .orElseThrow(() -> new ResourceNotFoundException("TeamMember", "userId", targetUserId));

        boolean isSelf = currentUserId.equals(targetUserId);

        if (isSelf) {
            if (currentMember.getRole() == TeamRole.OWNER) {
                throw new BadRequestException("Team owner cannot leave the team without transferring ownership or deleting the team");
            }
        } else {
            if (currentMember.getRole() == TeamRole.MEMBER) {
                throw new ForbiddenException("Members cannot remove other members from the team");
            }
            if (currentMember.getRole() == TeamRole.ADMIN && targetMember.getRole() != TeamRole.MEMBER) {
                throw new ForbiddenException("Administrators can only remove regular members");
            }
        }

        teamMemberRepository.delete(targetMember);
        teamEventPublisher.publish(TeamMemberRemovedEvent.of(teamId, targetUserId));
    }

    @Transactional
    public void deleteTeam(UUID teamId, UUID currentUserId) {
        TeamMember currentMember = teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId)
            .orElseThrow(() -> new ForbiddenException("You are not a member of this team"));

        if (currentMember.getRole() != TeamRole.OWNER) {
            throw new ForbiddenException("Only the team owner can delete the team");
        }

        Team team = teamRepository.findById(teamId)
            .orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));

        teamRepository.delete(team);
        teamEventPublisher.publish(TeamDeletedEvent.of(teamId, currentUserId));
    }
}
