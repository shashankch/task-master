package com.taskmaster.notification.application.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.taskmaster.collaboration.domain.event.TaskCommentCreatedEvent;
import com.taskmaster.notification.application.service.NotificationService;
import com.taskmaster.notification.domain.model.NotificationType;
import com.taskmaster.task.domain.event.TaskAssignedEvent;
import com.taskmaster.task.domain.event.TaskStatusChangedEvent;
import com.taskmaster.task.domain.model.Task;
import com.taskmaster.task.domain.model.TaskStatus;
import com.taskmaster.task.domain.port.TaskRepository;
import com.taskmaster.team.domain.event.TeamMemberJoinedEvent;
import com.taskmaster.team.domain.model.Team;
import com.taskmaster.team.domain.model.TeamRole;
import com.taskmaster.team.domain.port.TeamRepository;
import com.taskmaster.user.domain.model.Role;
import com.taskmaster.user.domain.model.User;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TeamRepository teamRepository;

    private NotificationEventListener listener;

    private User creator;
    private User assignee;
    private Task task;

    @BeforeEach
    void setUp() {
        listener = new NotificationEventListener(notificationService, taskRepository, teamRepository);

        creator = new User("creator@example.com", "creator", "pw", "Creator", Role.USER);
        creator.setId(UUID.randomUUID());

        assignee = new User("assignee@example.com", "assignee", "pw", "Assignee", Role.USER);
        assignee.setId(UUID.randomUUID());

        task = new Task("Implement WebSocket Broker", "Real-time push", null, null, creator, assignee, null, null);
        task.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Should send notification when task is assigned")
    void handleTaskAssigned_ShouldNotifyAssignee() {
        when(taskRepository.findByIdAndNotDeleted(task.getId())).thenReturn(Optional.of(task));

        TaskAssignedEvent event = TaskAssignedEvent.of(task.getId(), creator.getId(), assignee.getId(), task.getTitle());
        listener.handleTaskAssigned(event);

        verify(notificationService).createAndSendNotification(
            eq(assignee.getId()),
            eq(NotificationType.TASK_ASSIGNED),
            eq("Task Assigned"),
            any(),
            any()
        );
    }

    @Test
    @DisplayName("Should send notification to task creator when comment is added by someone else")
    void handleCommentCreated_ShouldNotifyCreator() {
        when(taskRepository.findByIdAndNotDeleted(task.getId())).thenReturn(Optional.of(task));

        TaskCommentCreatedEvent event = TaskCommentCreatedEvent.of(
            UUID.randomUUID(),
            task.getId(),
            assignee.getId(),
            null
        );
        listener.handleCommentCreated(event);

        verify(notificationService).createAndSendNotification(
            eq(creator.getId()),
            eq(NotificationType.COMMENT_ADDED),
            eq("New Comment on Task"),
            any(),
            any()
        );
    }

    @Test
    @DisplayName("Should send notification when task status changes")
    void handleTaskStatusChanged_ShouldNotifyCreator() {
        when(taskRepository.findByIdAndNotDeleted(task.getId())).thenReturn(Optional.of(task));

        TaskStatusChangedEvent event = TaskStatusChangedEvent.of(
            task.getId(),
            assignee.getId(),
            assignee.getId(),
            TaskStatus.OPEN,
            TaskStatus.IN_PROGRESS
        );
        listener.handleTaskStatusChanged(event);

        verify(notificationService).createAndSendNotification(
            eq(creator.getId()),
            eq(NotificationType.TASK_UPDATED),
            eq("Task Status Updated"),
            any(),
            any()
        );
    }

    @Test
    @DisplayName("Should send notification to team owner when new member joins")
    void handleTeamMemberJoined_ShouldNotifyOwner() {
        Team team = new Team("Dev Team", "Desc", creator);
        team.setId(UUID.randomUUID());

        when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));

        TeamMemberJoinedEvent event = TeamMemberJoinedEvent.of(team.getId(), assignee.getId(), TeamRole.MEMBER);
        listener.handleTeamMemberJoined(event);

        verify(notificationService).createAndSendNotification(
            eq(creator.getId()),
            eq(NotificationType.TEAM_INVITE),
            eq("New Team Member"),
            any(),
            any()
        );
    }
}
