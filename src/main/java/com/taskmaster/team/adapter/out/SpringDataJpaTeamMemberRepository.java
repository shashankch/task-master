package com.taskmaster.team.adapter.out;

import com.taskmaster.team.domain.model.TeamMember;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataJpaTeamMemberRepository extends JpaRepository<TeamMember, UUID> {

    Optional<TeamMember> findByTeamIdAndUserId(UUID teamId, UUID userId);

    List<TeamMember> findAllByTeamIdOrderByJoinedAtAsc(UUID teamId);

    boolean existsByTeamIdAndUserId(UUID teamId, UUID userId);

    @Query("SELECT tm.team.id FROM TeamMember tm WHERE tm.user.id = :userId")
    List<UUID> findTeamIdsByUserId(@Param("userId") UUID userId);
}
