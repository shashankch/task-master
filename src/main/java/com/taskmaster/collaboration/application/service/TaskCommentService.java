package com.taskmaster.collaboration.application.service;

import com.taskmaster.collaboration.application.dto.CommentResponse;
import com.taskmaster.collaboration.application.dto.CreateCommentRequest;
import com.taskmaster.collaboration.application.dto.UpdateCommentRequest;
import com.taskmaster.collaboration.application.mapper.CommentMapper;
import com.taskmaster.collaboration.domain.event.TaskCommentCreatedEvent;
import com.taskmaster.collaboration.domain.event.TaskCommentDeletedEvent;
import com.taskmaster.collaboration.domain.event.TaskCommentUpdatedEvent;
import com.taskmaster.collaboration.domain.model.TaskComment;
import com.taskmaster.collaboration.domain.port.CollaborationEventPublisher;
import com.taskmaster.collaboration.domain.port.TaskCommentRepository;
import com.taskmaster.shared.exception.BadRequestException;
import com.taskmaster.shared.exception.ForbiddenException;
import com.taskmaster.shared.exception.ResourceNotFoundException;
import com.taskmaster.task.domain.model.Task;
import com.taskmaster.task.domain.port.TaskRepository;
import com.taskmaster.team.domain.port.TeamMemberRepository;
import com.taskmaster.user.domain.model.User;
import com.taskmaster.user.domain.port.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service managing threaded task comments and discussions.
 */
@Service
public class TaskCommentService {

    private final TaskCommentRepository taskCommentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final CommentMapper commentMapper;
    private final CollaborationEventPublisher eventPublisher;

    public TaskCommentService(
        TaskCommentRepository taskCommentRepository,
        TaskRepository taskRepository,
        UserRepository userRepository,
        TeamMemberRepository teamMemberRepository,
        CommentMapper commentMapper,
        CollaborationEventPublisher eventPublisher
    ) {
        this.taskCommentRepository = taskCommentRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.commentMapper = commentMapper;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CommentResponse createComment(UUID taskId, UUID authorId, CreateCommentRequest request) {
        Task task = taskRepository.findByIdAndNotDeleted(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        if (task.getTeamId() != null && !teamMemberRepository.existsByTeamIdAndUserId(task.getTeamId(), authorId)) {
            throw new ForbiddenException("You are not a member of the team this task belongs to");
        }

        User author = userRepository.findById(authorId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", authorId));

        TaskComment parentComment = null;
        if (request.parentCommentId() != null) {
            parentComment = taskCommentRepository.findById(request.parentCommentId())
                .orElseThrow(() -> new ResourceNotFoundException("TaskComment", "id", request.parentCommentId()));

            if (!parentComment.getTask().getId().equals(taskId)) {
                throw new BadRequestException("Parent comment does not belong to this task");
            }
        }

        TaskComment comment = new TaskComment(task, author, parentComment, request.content());
        if (parentComment != null) {
            parentComment.getReplies().add(comment);
        }
        TaskComment savedComment = taskCommentRepository.save(comment);

        eventPublisher.publish(TaskCommentCreatedEvent.of(
            savedComment.getId(),
            task.getId(),
            author.getId(),
            parentComment != null ? parentComment.getId() : null
        ));

        return commentMapper.toResponse(savedComment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getTaskComments(UUID taskId, UUID currentUserId) {
        Task task = taskRepository.findByIdAndNotDeleted(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        if (task.getTeamId() != null && !teamMemberRepository.existsByTeamIdAndUserId(task.getTeamId(), currentUserId)) {
            throw new ForbiddenException("You are not a member of the team this task belongs to");
        }

        List<TaskComment> rootComments = taskCommentRepository.findRootCommentsByTaskId(taskId);
        return rootComments.stream().map(commentMapper::toResponse).toList();
    }

    @Transactional
    public CommentResponse updateComment(UUID commentId, UUID currentUserId, UpdateCommentRequest request) {
        TaskComment comment = taskCommentRepository.findById(commentId)
            .orElseThrow(() -> new ResourceNotFoundException("TaskComment", "id", commentId));

        if (comment.isDeleted()) {
            throw new BadRequestException("Cannot edit a deleted comment");
        }

        if (!comment.getAuthor().getId().equals(currentUserId)) {
            throw new ForbiddenException("You can only edit your own comments");
        }

        comment.editContent(request.content());
        TaskComment updated = taskCommentRepository.save(comment);

        eventPublisher.publish(TaskCommentUpdatedEvent.of(comment.getId(), comment.getTask().getId(), currentUserId));

        return commentMapper.toResponse(updated);
    }

    @Transactional
    public void deleteComment(UUID commentId, UUID currentUserId) {
        TaskComment comment = taskCommentRepository.findById(commentId)
            .orElseThrow(() -> new ResourceNotFoundException("TaskComment", "id", commentId));

        if (!comment.getAuthor().getId().equals(currentUserId)) {
            throw new ForbiddenException("You can only delete your own comments");
        }

        comment.softDelete();
        taskCommentRepository.save(comment);

        eventPublisher.publish(TaskCommentDeletedEvent.of(comment.getId(), comment.getTask().getId(), currentUserId));
    }
}
