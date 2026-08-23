package com.taskmaster.team.adapter.out;

import com.taskmaster.team.domain.model.Team;
import com.taskmaster.team.domain.port.TeamRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class JpaTeamRepository implements TeamRepository {

    private final SpringDataJpaTeamRepository repository;

    public JpaTeamRepository(SpringDataJpaTeamRepository repository) {
        this.repository = repository;
    }

    @Override
    public Team save(Team team) {
        return repository.save(team);
    }

    @Override
    public Optional<Team> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public Optional<Team> findByInviteCode(String inviteCode) {
        return repository.findByInviteCode(inviteCode);
    }

    @Override
    public List<Team> findAllByUserId(UUID userId) {
        return repository.findAllByUserId(userId);
    }

    @Override
    public boolean existsById(UUID id) {
        return repository.existsById(id);
    }

    @Override
    public void delete(Team team) {
        repository.delete(team);
    }
}
