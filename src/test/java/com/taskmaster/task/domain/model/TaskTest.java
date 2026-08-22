package com.taskmaster.task.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.taskmaster.shared.exception.BadRequestException;
import com.taskmaster.user.domain.model.Role;
import com.taskmaster.user.domain.model.User;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TaskTest {

    private User creator;
    private User assignee;

    @BeforeEach
    void setUp() {
        creator = new User("creator@example.com", "creator", "pw", "Creator", Role.USER);
        creator.setId(UUID.randomUUID());
        assignee = new User("assignee@example.com", "assignee", "pw", "Assignee", Role.USER);
        assignee.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Should initialize task with OPEN status and MEDIUM priority by default")
    void initializeTask_ShouldHaveDefaultValues() {
        Task task = new Task("Test Task", "Description", null, null, creator, null, null, null);

        assertThat(task.getStatus()).isEqualTo(TaskStatus.OPEN);
        assertThat(task.getPriority()).isEqualTo(TaskPriority.MEDIUM);
        assertThat(task.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("Should allow valid status transitions in lifecycle state machine")
    void updateStatus_WhenValidTransitions_ShouldSucceed() {
        Task task = new Task("Test Task", "Description", TaskPriority.HIGH, null, creator, assignee, null, Set.of("backend"));

        // OPEN -> IN_PROGRESS
        task.updateStatus(TaskStatus.IN_PROGRESS);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);

        // IN_PROGRESS -> REVIEW
        task.updateStatus(TaskStatus.REVIEW);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.REVIEW);

        // REVIEW -> COMPLETED
        task.updateStatus(TaskStatus.COMPLETED);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED);

        // COMPLETED -> ARCHIVED
        task.updateStatus(TaskStatus.ARCHIVED);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.ARCHIVED);

        // ARCHIVED -> OPEN
        task.updateStatus(TaskStatus.OPEN);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.OPEN);
    }

    @Test
    @DisplayName("Should throw BadRequestException on invalid status transitions")
    void updateStatus_WhenInvalidTransition_ShouldThrow() {
        Task task = new Task("Test Task", "Description", TaskPriority.HIGH, null, creator, assignee, null, null);

        // OPEN -> COMPLETED (invalid, must go through IN_PROGRESS and REVIEW)
        assertThatThrownBy(() -> task.updateStatus(TaskStatus.COMPLETED))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("Invalid status transition from OPEN to COMPLETED");
    }

    @Test
    @DisplayName("Should soft delete task by setting deletedAt timestamp")
    void softDelete_ShouldSetDeletedAt() {
        Task task = new Task("Test Task", "Description", TaskPriority.HIGH, null, creator, assignee, null, null);
        assertThat(task.isDeleted()).isFalse();

        task.softDelete();

        assertThat(task.isDeleted()).isTrue();
        assertThat(task.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should assign and unassign user")
    void assignAndUnassign_ShouldUpdateAssignee() {
        Task task = new Task("Test Task", "Description", TaskPriority.HIGH, null, creator, null, null, null);
        assertThat(task.getAssignee()).isNull();

        task.assignTo(assignee);
        assertThat(task.getAssignee()).isEqualTo(assignee);

        task.unassign();
        assertThat(task.getAssignee()).isNull();
    }
}
