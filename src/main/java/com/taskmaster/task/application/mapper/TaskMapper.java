package com.taskmaster.task.application.mapper;

import com.taskmaster.task.application.dto.TaskResponse;
import com.taskmaster.task.domain.model.Task;
import com.taskmaster.user.application.mapper.UserMapper;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {

    private final UserMapper userMapper;

    public TaskMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public TaskResponse toResponse(Task task) {
        if (task == null) {
            return null;
        }

        return new TaskResponse(
            task.getId(),
            task.getTitle(),
            task.getDescription(),
            task.getStatus(),
            task.getPriority(),
            task.getDueDate(),
            userMapper.toResponse(task.getCreator()),
            task.getAssignee() != null ? userMapper.toResponse(task.getAssignee()) : null,
            task.getTeamId(),
            task.getLabels(),
            task.getVersion(),
            task.getCreatedAt(),
            task.getUpdatedAt()
        );
    }
}
