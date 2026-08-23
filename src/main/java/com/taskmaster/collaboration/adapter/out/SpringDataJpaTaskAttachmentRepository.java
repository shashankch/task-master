package com.taskmaster.collaboration.adapter.out;

import com.taskmaster.collaboration.domain.model.TaskAttachment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataJpaTaskAttachmentRepository extends JpaRepository<TaskAttachment, UUID> {

    List<TaskAttachment> findAllByTaskIdOrderByCreatedAtDesc(UUID taskId);
}
