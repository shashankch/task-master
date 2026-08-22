package com.taskmaster.team.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.taskmaster.team.domain.event.TeamMemberRoleUpdatedEvent;
import com.taskmaster.team.domain.model.Team;
import com.taskmaster.team.domain.model.TeamMember;
import com.taskmaster.team.domain.model.TeamRole;
import com.taskmaster.team.domain.port.TeamEventPublisher;
import com.taskmaster.team.domain.port.TeamMemberRepository;
import com.taskmaster.team.domain.port.TeamRepository;
import com.taskmaster.user.application.mapper.UserMapper;
import com.taskmaster.user.domain.model.Role;
import com.taskmaster.user.domain.model.User;
import com.taskmaster.user.domain.port.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TeamEventPublisher teamEventPublisher;

    private TeamMapper teamMapper;
    private TeamService teamService;

    private User owner;
    private User memberUser;

    @BeforeEach
    void setUp() {
        UserMapper userMapper = new UserMapper();
        teamMapper = new TeamMapper(userMapper);
        teamService = new TeamService(teamRepository, teamMemberRepository, userRepository, teamMapper, teamEventPublisher);

        owner = new User("owner@example.com", "owner", "pw", "Owner", Role.USER);
        owner.setId(UUID.randomUUID());

        memberUser = new User("member@example.com", "member", "pw", "Member", Role.USER);
        memberUser.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Should create team and assign creator as OWNER")
    void createTeam_WhenValid_ShouldCreateAndPublishEvent() {
        CreateTeamRequest request = new CreateTeamRequest("Platform Engineering", "Core infra team");
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));

        Team team = new Team("Platform Engineering", "Core infra team", owner);
        team.setId(UUID.randomUUID());
        team.addMember(owner, TeamRole.OWNER);

        when(teamRepository.save(any(Team.class))).thenReturn(team);

        TeamResponse response = teamService.createTeam(owner.getId(), request);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Platform Engineering");
        assertThat(response.owner().id()).isEqualTo(owner.getId());

        verify(teamEventPublisher).publish(any(TeamCreatedEvent.class));
        verify(teamEventPublisher).publish(any(TeamMemberJoinedEvent.class));
    }

    @Test
    @DisplayName("Should retrieve team details for active member")
    void getTeamById_WhenMember_ShouldReturnDetail() {
        UUID teamId = UUID.randomUUID();
        Team team = new Team("Dev Team", "Desc", owner);
        team.setId(teamId);
        TeamMember member = new TeamMember(team, owner, TeamRole.OWNER);

        when(teamMemberRepository.findByTeamIdAndUserId(teamId, owner.getId())).thenReturn(Optional.of(member));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));

        TeamDetailResponse response = teamService.getTeamById(teamId, owner.getId());

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Dev Team");
    }

    @Test
    @DisplayName("Should throw ForbiddenException when non-member requests team details")
    void getTeamById_WhenNotMember_ShouldThrowForbidden() {
        UUID teamId = UUID.randomUUID();
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, memberUser.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.getTeamById(teamId, memberUser.getId()))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("You are not a member of this team");
    }

    @Test
    @DisplayName("Should allow user to join team with valid invite code")
    void joinTeamByInviteCode_WhenValid_ShouldAddMember() {
        Team team = new Team("Dev Team", "Desc", owner);
        team.setId(UUID.randomUUID());

        when(teamRepository.findByInviteCode(team.getInviteCode())).thenReturn(Optional.of(team));
        when(teamMemberRepository.existsByTeamIdAndUserId(team.getId(), memberUser.getId())).thenReturn(false);
        when(userRepository.findById(memberUser.getId())).thenReturn(Optional.of(memberUser));

        JoinTeamRequest request = new JoinTeamRequest(team.getInviteCode());
        TeamResponse response = teamService.joinTeamByInviteCode(memberUser.getId(), request);

        assertThat(response).isNotNull();
        verify(teamMemberRepository).save(any(TeamMember.class));
        verify(teamEventPublisher).publish(any(TeamMemberJoinedEvent.class));
    }

    @Test
    @DisplayName("Should throw ConflictException if user already in team")
    void joinTeamByInviteCode_WhenAlreadyMember_ShouldThrowConflict() {
        Team team = new Team("Dev Team", "Desc", owner);
        team.setId(UUID.randomUUID());

        when(teamRepository.findByInviteCode(team.getInviteCode())).thenReturn(Optional.of(team));
        when(teamMemberRepository.existsByTeamIdAndUserId(team.getId(), owner.getId())).thenReturn(true);

        JoinTeamRequest request = new JoinTeamRequest(team.getInviteCode());
        assertThatThrownBy(() -> teamService.joinTeamByInviteCode(owner.getId(), request))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("You are already a member of this team");
    }

    @Test
    @DisplayName("Should allow OWNER to update member role to ADMIN")
    void updateMemberRole_WhenOwner_ShouldUpdateRole() {
        UUID teamId = UUID.randomUUID();
        Team team = new Team("Dev Team", "Desc", owner);
        team.setId(teamId);

        TeamMember ownerMember = new TeamMember(team, owner, TeamRole.OWNER);
        TeamMember targetMember = new TeamMember(team, memberUser, TeamRole.MEMBER);

        when(teamMemberRepository.findByTeamIdAndUserId(teamId, owner.getId())).thenReturn(Optional.of(ownerMember));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, memberUser.getId())).thenReturn(Optional.of(targetMember));
        when(teamMemberRepository.save(targetMember)).thenReturn(targetMember);

        UpdateMemberRoleRequest request = new UpdateMemberRoleRequest(TeamRole.ADMIN);
        TeamMemberResponse response = teamService.updateMemberRole(teamId, memberUser.getId(), owner.getId(), request);

        assertThat(response.role()).isEqualTo(TeamRole.ADMIN);
        verify(teamEventPublisher).publish(any(TeamMemberRoleUpdatedEvent.class));
    }

    @Test
    @DisplayName("Should delete team when requested by OWNER")
    void deleteTeam_WhenOwner_ShouldDelete() {
        UUID teamId = UUID.randomUUID();
        Team team = new Team("Dev Team", "Desc", owner);
        team.setId(teamId);
        TeamMember ownerMember = new TeamMember(team, owner, TeamRole.OWNER);

        when(teamMemberRepository.findByTeamIdAndUserId(teamId, owner.getId())).thenReturn(Optional.of(ownerMember));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));

        teamService.deleteTeam(teamId, owner.getId());

        verify(teamRepository).delete(team);
        verify(teamEventPublisher).publish(any(TeamDeletedEvent.class));
    }
}
