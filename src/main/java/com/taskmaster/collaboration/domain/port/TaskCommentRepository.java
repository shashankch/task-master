package com.taskmaster.collaboration.domain.port;

import com.taskmaster.collaboration.domain.model.TaskComment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskCommentRepository {

    TaskComment save(TaskComment comment);

    Optional<TaskComment> findById(UUID id);

    List<TaskComment> findRootCommentsByTaskId(UUID taskId);

    void delete(TaskComment comment);
}
