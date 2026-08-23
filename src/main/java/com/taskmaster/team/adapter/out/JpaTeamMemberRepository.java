package com.taskmaster.team.adapter.out;

import com.taskmaster.team.domain.model.TeamMember;
import com.taskmaster.team.domain.port.TeamMemberRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class JpaTeamMemberRepository implements TeamMemberRepository {

    private final SpringDataJpaTeamMemberRepository repository;

    public JpaTeamMemberRepository(SpringDataJpaTeamMemberRepository repository) {
        this.repository = repository;
    }

    @Override
    public TeamMember save(TeamMember member) {
        return repository.save(member);
    }

    @Override
    public Optional<TeamMember> findByTeamIdAndUserId(UUID teamId, UUID userId) {
        return repository.findByTeamIdAndUserId(teamId, userId);
    }

    @Override
    public List<TeamMember> findAllByTeamId(UUID teamId) {
        return repository.findAllByTeamIdOrderByJoinedAtAsc(teamId);
    }

    @Override
    public boolean existsByTeamIdAndUserId(UUID teamId, UUID userId) {
        return repository.existsByTeamIdAndUserId(teamId, userId);
    }

    @Override
    public void delete(TeamMember member) {
        repository.delete(member);
    }
}
