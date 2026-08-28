package com.taskmaster.task.application.service;

import com.taskmaster.shared.dto.PageResponse;
import com.taskmaster.shared.exception.ForbiddenException;
import com.taskmaster.shared.exception.ResourceNotFoundException;
import com.taskmaster.task.adapter.out.TaskSpecification;
import com.taskmaster.task.application.dto.AssignTaskRequest;
import com.taskmaster.task.application.dto.CreateTaskRequest;
import com.taskmaster.task.application.dto.TaskFilterCriteria;
import com.taskmaster.task.application.dto.TaskResponse;
import com.taskmaster.task.application.dto.UpdateTaskRequest;
import com.taskmaster.task.application.dto.UpdateTaskStatusRequest;
import com.taskmaster.task.application.mapper.TaskMapper;
import com.taskmaster.task.domain.event.TaskAssignedEvent;
import com.taskmaster.task.domain.event.TaskCreatedEvent;
import com.taskmaster.task.domain.event.TaskDeletedEvent;
import com.taskmaster.task.domain.event.TaskStatusChangedEvent;
import com.taskmaster.task.domain.event.TaskUpdatedEvent;
import com.taskmaster.task.domain.model.Task;
import com.taskmaster.task.domain.model.TaskStatus;
import com.taskmaster.task.domain.port.TaskEventPublisher;
import com.taskmaster.task.domain.port.TaskRepository;
import com.taskmaster.team.domain.port.TeamMemberRepository;
import com.taskmaster.user.domain.model.User;
import com.taskmaster.user.domain.port.UserRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service managing Task lifecycle, querying, state transitions, and assignment with team authorization gates.
 */
@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TaskMapper taskMapper;
    private final TaskEventPublisher taskEventPublisher;

    public TaskService(
        TaskRepository taskRepository,
        UserRepository userRepository,
        TeamMemberRepository teamMemberRepository,
        TaskMapper taskMapper,
        TaskEventPublisher taskEventPublisher
    ) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.taskMapper = taskMapper;
        this.taskEventPublisher = taskEventPublisher;
    }

    @Transactional
    public TaskResponse createTask(UUID creatorId, CreateTaskRequest request) {
        User creator = userRepository.findById(creatorId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", creatorId));

        if (request.teamId() != null && !teamMemberRepository.existsByTeamIdAndUserId(request.teamId(), creatorId)) {
            throw new ForbiddenException("You are not a member of the team specified for this task");
        }

        User assignee = null;
        if (request.assigneeId() != null) {
            assignee = userRepository.findById(request.assigneeId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.assigneeId()));
        }

        Task task = new Task(
            request.title().trim(),
            request.description(),
            request.priority(),
            request.dueDate(),
            creator,
            assignee,
            request.teamId(),
            request.labels()
        );

        Task savedTask = taskRepository.save(task);

        taskEventPublisher.publish(TaskCreatedEvent.of(
            savedTask.getId(),
            creator.getId(),
            assignee != null ? assignee.getId() : null,
            savedTask.getTitle()
        ));

        if (assignee != null) {
            taskEventPublisher.publish(TaskAssignedEvent.of(
                savedTask.getId(),
                creator.getId(),
                assignee.getId(),
                savedTask.getTitle()
            ));
        }

        return taskMapper.toResponse(savedTask);
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskById(UUID taskId, UUID currentUserId) {
        Task task = taskRepository.findByIdAndNotDeleted(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        if (task.getTeamId() != null && currentUserId != null) {
            if (!teamMemberRepository.existsByTeamIdAndUserId(task.getTeamId(), currentUserId)) {
                throw new ForbiddenException("You are not a member of the team this task belongs to");
            }
        }

        return taskMapper.toResponse(task);
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskById(UUID taskId) {
        return getTaskById(taskId, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> searchTasks(TaskFilterCriteria criteria, Pageable pageable) {
        Specification<Task> spec = TaskSpecification.withFilter(criteria);
        Page<Task> page = taskRepository.findAll(spec, pageable);
        return PageResponse.from(page.map(taskMapper::toResponse));
    }

    @Transactional
    public TaskResponse updateTask(UUID taskId, UUID updaterId, UpdateTaskRequest request) {
        Task task = taskRepository.findByIdAndNotDeleted(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        if (task.getTeamId() != null && !teamMemberRepository.existsByTeamIdAndUserId(task.getTeamId(), updaterId)) {
            throw new ForbiddenException("You are not a member of the team this task belongs to");
        }

        task.updateDetails(
            request.title(),
            request.description(),
            request.priority(),
            request.dueDate(),
            request.labels()
        );

        Task updatedTask = taskRepository.save(task);

        taskEventPublisher.publish(TaskUpdatedEvent.of(
            updatedTask.getId(),
            updaterId,
            updatedTask.getTitle()
        ));

        return taskMapper.toResponse(updatedTask);
    }

    @Transactional
    public TaskResponse updateTaskStatus(UUID taskId, UUID changerId, UpdateTaskStatusRequest request) {
        Task task = taskRepository.findByIdAndNotDeleted(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        if (task.getTeamId() != null && !teamMemberRepository.existsByTeamIdAndUserId(task.getTeamId(), changerId)) {
            throw new ForbiddenException("You are not a member of the team this task belongs to");
        }

        TaskStatus oldStatus = task.getStatus();
        task.updateStatus(request.status());

        Task updatedTask = taskRepository.save(task);

        taskEventPublisher.publish(TaskStatusChangedEvent.of(
            updatedTask.getId(),
            changerId,
            updatedTask.getAssignee() != null ? updatedTask.getAssignee().getId() : null,
            oldStatus,
            request.status()
        ));

        return taskMapper.toResponse(updatedTask);
    }

    @Transactional
    public TaskResponse assignTask(UUID taskId, UUID assignerId, AssignTaskRequest request) {
        Task task = taskRepository.findByIdAndNotDeleted(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        if (task.getTeamId() != null && !teamMemberRepository.existsByTeamIdAndUserId(task.getTeamId(), assignerId)) {
            throw new ForbiddenException("You are not a member of the team this task belongs to");
        }

        if (request.assigneeId() == null) {
            task.unassign();
        } else {
            User assignee = userRepository.findById(request.assigneeId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.assigneeId()));

            if (task.getTeamId() != null && !teamMemberRepository.existsByTeamIdAndUserId(task.getTeamId(), assignee.getId())) {
                throw new ForbiddenException("Assignee must be a member of the team this task belongs to");
            }

            task.assignTo(assignee);

            taskEventPublisher.publish(TaskAssignedEvent.of(
                task.getId(),
                assignerId,
                assignee.getId(),
                task.getTitle()
            ));
        }

        Task updatedTask = taskRepository.save(task);
        return taskMapper.toResponse(updatedTask);
    }

    @Transactional
    public void deleteTask(UUID taskId, UUID deleterId) {
        Task task = taskRepository.findByIdAndNotDeleted(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        if (task.getTeamId() != null && !teamMemberRepository.existsByTeamIdAndUserId(task.getTeamId(), deleterId)) {
            throw new ForbiddenException("You are not a member of the team this task belongs to");
        }

        task.softDelete();
        taskRepository.save(task);

        taskEventPublisher.publish(TaskDeletedEvent.of(taskId, deleterId));
    }
}
