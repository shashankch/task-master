package com.taskmaster.collaboration.adapter.out;

import com.taskmaster.collaboration.domain.model.TaskAttachment;
import com.taskmaster.collaboration.domain.port.TaskAttachmentRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class JpaTaskAttachmentRepository implements TaskAttachmentRepository {

    private final SpringDataJpaTaskAttachmentRepository repository;

    public JpaTaskAttachmentRepository(SpringDataJpaTaskAttachmentRepository repository) {
        this.repository = repository;
    }

    @Override
    public TaskAttachment save(TaskAttachment attachment) {
        return repository.save(attachment);
    }

    @Override
    public Optional<TaskAttachment> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public List<TaskAttachment> findAllByTaskId(UUID taskId) {
        return repository.findAllByTaskIdOrderByCreatedAtDesc(taskId);
    }

    @Override
    public void delete(TaskAttachment attachment) {
        repository.delete(attachment);
    }
}
