package com.taskmaster.collaboration.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.taskmaster.collaboration.application.dto.CommentResponse;
import com.taskmaster.collaboration.application.dto.CreateCommentRequest;
import com.taskmaster.collaboration.application.dto.UpdateCommentRequest;
import com.taskmaster.collaboration.application.mapper.CommentMapper;
import com.taskmaster.collaboration.domain.event.TaskCommentCreatedEvent;
import com.taskmaster.collaboration.domain.event.TaskCommentDeletedEvent;
import com.taskmaster.collaboration.domain.model.TaskComment;
import com.taskmaster.collaboration.domain.port.CollaborationEventPublisher;
import com.taskmaster.collaboration.domain.port.TaskCommentRepository;
import com.taskmaster.shared.exception.ForbiddenException;
import com.taskmaster.task.domain.model.Task;
import com.taskmaster.task.domain.port.TaskRepository;
import com.taskmaster.team.domain.port.TeamMemberRepository;
import com.taskmaster.user.application.mapper.UserMapper;
import com.taskmaster.user.domain.model.Role;
import com.taskmaster.user.domain.model.User;
import com.taskmaster.user.domain.port.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskCommentServiceTest {

    @Mock
    private TaskCommentRepository taskCommentRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private CollaborationEventPublisher eventPublisher;

    private CommentMapper commentMapper;
    private TaskCommentService commentService;

    private User author;
    private User otherUser;
    private Task task;

    @BeforeEach
    void setUp() {
        UserMapper userMapper = new UserMapper();
        commentMapper = new CommentMapper(userMapper);
        commentService = new TaskCommentService(
            taskCommentRepository,
            taskRepository,
            userRepository,
            teamMemberRepository,
            commentMapper,
            eventPublisher
        );

        author = new User("author@example.com", "author", "pw", "Author", Role.USER);
        author.setId(UUID.randomUUID());

        otherUser = new User("other@example.com", "other", "pw", "Other", Role.USER);
        otherUser.setId(UUID.randomUUID());

        task = new Task("Task Title", "Task Desc", null, null, author, null, null, null);
        task.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Should create root comment and publish domain event")
    void createComment_WhenRootComment_ShouldSaveAndPublish() {
        CreateCommentRequest request = new CreateCommentRequest("Great work on this task!", null);

        when(taskRepository.findByIdAndNotDeleted(task.getId())).thenReturn(Optional.of(task));
        when(userRepository.findById(author.getId())).thenReturn(Optional.of(author));

        TaskComment savedComment = new TaskComment(task, author, null, "Great work on this task!");
        savedComment.setId(UUID.randomUUID());
        when(taskCommentRepository.save(any(TaskComment.class))).thenReturn(savedComment);

        CommentResponse response = commentService.createComment(task.getId(), author.getId(), request);

        assertThat(response).isNotNull();
        assertThat(response.content()).isEqualTo("Great work on this task!");
        verify(eventPublisher).publish(any(TaskCommentCreatedEvent.class));
    }

    @Test
    @DisplayName("Should update comment content when edited by author")
    void updateComment_WhenAuthor_ShouldUpdate() {
        UUID commentId = UUID.randomUUID();
        TaskComment comment = new TaskComment(task, author, null, "Initial comment");
        comment.setId(commentId);

        when(taskCommentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(taskCommentRepository.save(comment)).thenReturn(comment);

        UpdateCommentRequest request = new UpdateCommentRequest("Edited comment");
        CommentResponse response = commentService.updateComment(commentId, author.getId(), request);

        assertThat(response.content()).isEqualTo("Edited comment");
    }

    @Test
    @DisplayName("Should throw ForbiddenException when editing comment by another user")
    void updateComment_WhenNotAuthor_ShouldThrowForbidden() {
        UUID commentId = UUID.randomUUID();
        TaskComment comment = new TaskComment(task, author, null, "Initial comment");
        comment.setId(commentId);

        when(taskCommentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        UpdateCommentRequest request = new UpdateCommentRequest("Edited comment");
        assertThatThrownBy(() -> commentService.updateComment(commentId, otherUser.getId(), request))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("You can only edit your own comments");
    }

    @Test
    @DisplayName("Should soft delete comment when deleted by author")
    void deleteComment_WhenAuthor_ShouldSoftDelete() {
        UUID commentId = UUID.randomUUID();
        TaskComment comment = new TaskComment(task, author, null, "Initial comment");
        comment.setId(commentId);

        when(taskCommentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(taskCommentRepository.save(comment)).thenReturn(comment);

        commentService.deleteComment(commentId, author.getId());

        assertThat(comment.isDeleted()).isTrue();
        verify(eventPublisher).publish(any(TaskCommentDeletedEvent.class));
    }
}
