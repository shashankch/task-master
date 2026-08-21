package com.taskmaster.task.domain.port;

import com.taskmaster.task.domain.model.Task;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

/**
 * Port interface for Task persistence and query operations.
 */
public interface TaskRepository {

    Task save(Task task);

    Optional<Task> findByIdAndNotDeleted(UUID id);

    Optional<Task> findById(UUID id);

    Page<Task> findAll(Specification<Task> spec, Pageable pageable);

    boolean existsByIdAndNotDeleted(UUID id);

    void delete(Task task);
}
