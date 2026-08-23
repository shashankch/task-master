package com.taskmaster.notification.application.listener;

import com.taskmaster.collaboration.domain.event.TaskCommentCreatedEvent;
import com.taskmaster.notification.application.service.NotificationService;
import com.taskmaster.notification.domain.model.NotificationType;
import com.taskmaster.task.domain.event.TaskAssignedEvent;
import com.taskmaster.task.domain.event.TaskStatusChangedEvent;
import com.taskmaster.task.domain.model.Task;
import com.taskmaster.task.domain.port.TaskRepository;
import com.taskmaster.team.domain.event.TeamMemberJoinedEvent;
import com.taskmaster.team.domain.model.Team;
import com.taskmaster.team.domain.port.TeamRepository;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Event listener consuming domain events across modules and dispatching user notifications.
 */
@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationService notificationService;
    private final TaskRepository taskRepository;
    private final TeamRepository teamRepository;

    public NotificationEventListener(
        NotificationService notificationService,
        TaskRepository taskRepository,
        TeamRepository teamRepository
    ) {
        this.notificationService = notificationService;
        this.taskRepository = taskRepository;
        this.teamRepository = teamRepository;
    }

    @EventListener
    public void handleTaskAssigned(TaskAssignedEvent event) {
        if (event.assigneeId() == null) {
            return;
        }

        Optional<Task> taskOpt = taskRepository.findByIdAndNotDeleted(event.taskId());
        if (taskOpt.isEmpty()) {
            return;
        }

        Task task = taskOpt.get();
        String title = "Task Assigned";
        String message = String.format("You have been assigned to task: %s", task.getTitle());
        String metadata = String.format("{\"taskId\":\"%s\"}", task.getId());

        notificationService.createAndSendNotification(
            event.assigneeId(),
            NotificationType.TASK_ASSIGNED,
            title,
            message,
            metadata
        );
        log.debug("Sent task assigned notification to user {}", event.assigneeId());
    }

    @EventListener
    public void handleCommentCreated(TaskCommentCreatedEvent event) {
        Optional<Task> taskOpt = taskRepository.findByIdAndNotDeleted(event.taskId());
        if (taskOpt.isEmpty()) {
            return;
        }

        Task task = taskOpt.get();
        String title = "New Comment on Task";
        String message = String.format("A new comment was posted on: %s", task.getTitle());
        String metadata = String.format("{\"taskId\":\"%s\",\"commentId\":\"%s\"}", task.getId(), event.commentId());

        // Notify Task Creator if not the commenter
        UUID creatorId = task.getCreator() != null ? task.getCreator().getId() : null;
        if (creatorId != null && !creatorId.equals(event.authorId())) {
            notificationService.createAndSendNotification(
                creatorId,
                NotificationType.COMMENT_ADDED,
                title,
                message,
                metadata
            );
        }

        // Notify Task Assignee if exists and not the commenter or creator
        UUID assigneeId = task.getAssignee() != null ? task.getAssignee().getId() : null;
        if (assigneeId != null && !assigneeId.equals(event.authorId()) && !assigneeId.equals(creatorId)) {
            notificationService.createAndSendNotification(
                assigneeId,
                NotificationType.COMMENT_ADDED,
                title,
                message,
                metadata
            );
        }
    }

    @EventListener
    public void handleTaskStatusChanged(TaskStatusChangedEvent event) {
        Optional<Task> taskOpt = taskRepository.findByIdAndNotDeleted(event.taskId());
        if (taskOpt.isEmpty()) {
            return;
        }

        Task task = taskOpt.get();
        String title = "Task Status Updated";
        String message = String.format("Task '%s' moved from %s to %s", task.getTitle(), event.oldStatus(), event.newStatus());
        String metadata = String.format("{\"taskId\":\"%s\",\"status\":\"%s\"}", task.getId(), event.newStatus());

        UUID creatorId = task.getCreator() != null ? task.getCreator().getId() : null;
        if (creatorId != null) {
            notificationService.createAndSendNotification(
                creatorId,
                NotificationType.TASK_UPDATED,
                title,
                message,
                metadata
            );
        }
    }

    @EventListener
    public void handleTeamMemberJoined(TeamMemberJoinedEvent event) {
        Optional<Team> teamOpt = teamRepository.findById(event.teamId());
        if (teamOpt.isEmpty()) {
            return;
        }

        Team team = teamOpt.get();
        UUID ownerId = team.getOwner() != null ? team.getOwner().getId() : null;

        if (ownerId != null && !ownerId.equals(event.userId())) {
            String title = "New Team Member";
            String message = String.format("A new member joined your workspace: %s", team.getName());
            String metadata = String.format("{\"teamId\":\"%s\",\"userId\":\"%s\"}", team.getId(), event.userId());

            notificationService.createAndSendNotification(
                ownerId,
                NotificationType.TEAM_INVITE,
                title,
                message,
                metadata
            );
        }
    }
}
