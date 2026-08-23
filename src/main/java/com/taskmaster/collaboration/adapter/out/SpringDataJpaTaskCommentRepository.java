package com.taskmaster.collaboration.adapter.out;

import com.taskmaster.collaboration.domain.model.TaskComment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataJpaTaskCommentRepository extends JpaRepository<TaskComment, UUID> {

    @Query("SELECT c FROM TaskComment c WHERE c.task.id = :taskId AND c.parentComment IS NULL ORDER BY c.createdAt ASC")
    List<TaskComment> findRootCommentsByTaskId(@Param("taskId") UUID taskId);
}
