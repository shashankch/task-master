package com.taskmaster.task.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.taskmaster.shared.exception.ResourceNotFoundException;
import com.taskmaster.task.application.dto.AssignTaskRequest;
import com.taskmaster.task.application.dto.CreateTaskRequest;
import com.taskmaster.task.application.dto.TaskResponse;
import com.taskmaster.task.application.dto.UpdateTaskRequest;
import com.taskmaster.task.application.dto.UpdateTaskStatusRequest;
import com.taskmaster.task.application.mapper.TaskMapper;
import com.taskmaster.task.domain.event.TaskCreatedEvent;
import com.taskmaster.task.domain.event.TaskDeletedEvent;
import com.taskmaster.task.domain.event.TaskStatusChangedEvent;
import com.taskmaster.task.domain.model.Task;
import com.taskmaster.task.domain.model.TaskPriority;
import com.taskmaster.task.domain.model.TaskStatus;
import com.taskmaster.task.domain.port.TaskEventPublisher;
import com.taskmaster.task.domain.port.TaskRepository;
import com.taskmaster.user.application.mapper.UserMapper;
import com.taskmaster.user.domain.model.Role;
import com.taskmaster.user.domain.model.User;
import com.taskmaster.user.domain.port.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.taskmaster.team.domain.port.TeamMemberRepository;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private TaskEventPublisher taskEventPublisher;

    private TaskMapper taskMapper;
    private TaskService taskService;

    private User creator;
    private User assignee;

    @BeforeEach
    void setUp() {
        UserMapper userMapper = new UserMapper();
        taskMapper = new TaskMapper(userMapper);
        taskService = new TaskService(taskRepository, userRepository, teamMemberRepository, taskMapper, taskEventPublisher);

        creator = new User("creator@example.com", "creator", "pw", "Creator", Role.USER);
        creator.setId(UUID.randomUUID());

        assignee = new User("assignee@example.com", "assignee", "pw", "Assignee", Role.USER);
        assignee.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Should create task and publish events")
    void createTask_WhenValid_ShouldSaveAndPublishEvents() {
        CreateTaskRequest request = new CreateTaskRequest(
            "New Task",
            "Task details",
            TaskPriority.HIGH,
            Instant.now().plusSeconds(3600),
            assignee.getId(),
            null,
            Set.of("feature")
        );

        when(userRepository.findById(creator.getId())).thenReturn(Optional.of(creator));
        when(userRepository.findById(assignee.getId())).thenReturn(Optional.of(assignee));

        Task savedTask = new Task("New Task", "Task details", TaskPriority.HIGH, null, creator, assignee, null, Set.of("feature"));
        savedTask.setId(UUID.randomUUID());

        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        TaskResponse response = taskService.createTask(creator.getId(), request);

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("New Task");
        assertThat(response.status()).isEqualTo(TaskStatus.OPEN);
        assertThat(response.priority()).isEqualTo(TaskPriority.HIGH);

        verify(taskEventPublisher).publish(any(TaskCreatedEvent.class));
    }

    @Test
    @DisplayName("Should update task status and publish status changed event")
    void updateTaskStatus_WhenValidTransition_ShouldUpdateAndPublish() {
        UUID taskId = UUID.randomUUID();
        Task task = new Task("Task Title", "Desc", TaskPriority.MEDIUM, null, creator, assignee, null, null);
        task.setId(taskId);

        when(taskRepository.findByIdAndNotDeleted(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        UpdateTaskStatusRequest request = new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS);
        TaskResponse response = taskService.updateTaskStatus(taskId, creator.getId(), request);

        assertThat(response.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        verify(taskEventPublisher).publish(any(TaskStatusChangedEvent.class));
    }

    @Test
    @DisplayName("Should assign task to user")
    void assignTask_WhenValid_ShouldUpdateAssignee() {
        UUID taskId = UUID.randomUUID();
        Task task = new Task("Task Title", "Desc", TaskPriority.MEDIUM, null, creator, null, null, null);
        task.setId(taskId);

        when(taskRepository.findByIdAndNotDeleted(taskId)).thenReturn(Optional.of(task));
        when(userRepository.findById(assignee.getId())).thenReturn(Optional.of(assignee));
        when(taskRepository.save(task)).thenReturn(task);

        AssignTaskRequest request = new AssignTaskRequest(assignee.getId());
        TaskResponse response = taskService.assignTask(taskId, creator.getId(), request);

        assertThat(response.assignee()).isNotNull();
        assertThat(response.assignee().id()).isEqualTo(assignee.getId());
    }

    @Test
    @DisplayName("Should soft delete task and publish deleted event")
    void deleteTask_WhenFound_ShouldSoftDelete() {
        UUID taskId = UUID.randomUUID();
        Task task = new Task("Task Title", "Desc", TaskPriority.MEDIUM, null, creator, assignee, null, null);
        task.setId(taskId);

        when(taskRepository.findByIdAndNotDeleted(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        taskService.deleteTask(taskId, creator.getId());

        assertThat(task.isDeleted()).isTrue();
        verify(taskEventPublisher).publish(any(TaskDeletedEvent.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when task not found")
    void getTaskById_WhenNotFound_ShouldThrow() {
        UUID taskId = UUID.randomUUID();
        when(taskRepository.findByIdAndNotDeleted(taskId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskById(taskId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Task with id '" + taskId + "' not found");
    }
}
