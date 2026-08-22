package com.taskmaster.collaboration.domain.port;

import com.taskmaster.collaboration.domain.model.TaskAttachment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskAttachmentRepository {

    TaskAttachment save(TaskAttachment attachment);

    Optional<TaskAttachment> findById(UUID id);

    List<TaskAttachment> findAllByTaskId(UUID taskId);

    void delete(TaskAttachment attachment);
}
