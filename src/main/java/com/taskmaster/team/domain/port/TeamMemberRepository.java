package com.taskmaster.team.domain.port;

import com.taskmaster.team.domain.model.TeamMember;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamMemberRepository {

    TeamMember save(TeamMember member);

    Optional<TeamMember> findByTeamIdAndUserId(UUID teamId, UUID userId);

    List<TeamMember> findAllByTeamId(UUID teamId);

    boolean existsByTeamIdAndUserId(UUID teamId, UUID userId);

    void delete(TeamMember member);
}
