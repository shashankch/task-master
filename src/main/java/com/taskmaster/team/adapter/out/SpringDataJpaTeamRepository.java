package com.taskmaster.team.adapter.out;

import com.taskmaster.team.domain.model.Team;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataJpaTeamRepository extends JpaRepository<Team, UUID> {

    Optional<Team> findByInviteCode(String inviteCode);

    @Query("SELECT DISTINCT t FROM Team t JOIN t.members m WHERE m.user.id = :userId ORDER BY t.createdAt DESC")
    List<Team> findAllByUserId(@Param("userId") UUID userId);
}
