package com.taskmaster.team.domain.port;

import com.taskmaster.team.domain.model.Team;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamRepository {

    Team save(Team team);

    Optional<Team> findById(UUID id);

    Optional<Team> findByInviteCode(String inviteCode);

    List<Team> findAllByUserId(UUID userId);

    boolean existsById(UUID id);

    void delete(Team team);
}
