package com.taskmaster.collaboration.adapter.out;

import com.taskmaster.collaboration.domain.model.TaskComment;
import com.taskmaster.collaboration.domain.port.TaskCommentRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class JpaTaskCommentRepository implements TaskCommentRepository {

    private final SpringDataJpaTaskCommentRepository repository;

    public JpaTaskCommentRepository(SpringDataJpaTaskCommentRepository repository) {
        this.repository = repository;
    }

    @Override
    public TaskComment save(TaskComment comment) {
        return repository.save(comment);
    }

    @Override
    public Optional<TaskComment> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public List<TaskComment> findRootCommentsByTaskId(UUID taskId) {
        return repository.findRootCommentsByTaskId(taskId);
    }

    @Override
    public void delete(TaskComment comment) {
        repository.delete(comment);
    }
}
