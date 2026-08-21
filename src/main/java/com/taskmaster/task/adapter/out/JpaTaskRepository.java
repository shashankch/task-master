package com.taskmaster.task.adapter.out;

import com.taskmaster.task.domain.model.Task;
import com.taskmaster.task.domain.port.TaskRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * Outbound adapter implementing TaskRepository port with Spring Data JPA.
 */
@Component
public class JpaTaskRepository implements TaskRepository {

    private final SpringDataJpaTaskRepository repository;

    public JpaTaskRepository(SpringDataJpaTaskRepository repository) {
        this.repository = repository;
    }

    @Override
    public Task save(Task task) {
        return repository.save(task);
    }

    @Override
    public Optional<Task> findByIdAndNotDeleted(UUID id) {
        return repository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public Optional<Task> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public Page<Task> findAll(Specification<Task> spec, Pageable pageable) {
        return repository.findAll(spec, pageable);
    }

    @Override
    public boolean existsByIdAndNotDeleted(UUID id) {
        return repository.existsByIdAndDeletedAtIsNull(id);
    }

    @Override
    public void delete(Task task) {
        repository.delete(task);
    }
}
