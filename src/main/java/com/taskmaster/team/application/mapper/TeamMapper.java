package com.taskmaster.team.application.mapper;

import com.taskmaster.team.application.dto.TeamDetailResponse;
import com.taskmaster.team.application.dto.TeamMemberResponse;
import com.taskmaster.team.application.dto.TeamResponse;
import com.taskmaster.team.domain.model.Team;
import com.taskmaster.team.domain.model.TeamMember;
import com.taskmaster.user.application.mapper.UserMapper;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TeamMapper {

    private final UserMapper userMapper;

    public TeamMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public TeamResponse toResponse(Team team) {
        if (team == null) {
            return null;
        }

        return new TeamResponse(
            team.getId(),
            team.getName(),
            team.getDescription(),
            userMapper.toResponse(team.getOwner()),
            team.getInviteCode(),
            team.getMembers() != null ? team.getMembers().size() : 0,
            team.getCreatedAt(),
            team.getUpdatedAt()
        );
    }

    public TeamDetailResponse toDetailResponse(Team team) {
        if (team == null) {
            return null;
        }

        List<TeamMemberResponse> memberResponses = team.getMembers() != null
            ? team.getMembers().stream().map(this::toMemberResponse).toList()
            : List.of();

        return new TeamDetailResponse(
            team.getId(),
            team.getName(),
            team.getDescription(),
            userMapper.toResponse(team.getOwner()),
            team.getInviteCode(),
            memberResponses,
            team.getCreatedAt(),
            team.getUpdatedAt()
        );
    }

    public TeamMemberResponse toMemberResponse(TeamMember member) {
        if (member == null) {
            return null;
        }

        return new TeamMemberResponse(
            member.getId(),
            userMapper.toResponse(member.getUser()),
            member.getRole(),
            member.getJoinedAt()
        );
    }
}
