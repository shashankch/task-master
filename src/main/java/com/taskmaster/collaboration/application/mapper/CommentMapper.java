package com.taskmaster.collaboration.application.mapper;

import com.taskmaster.collaboration.application.dto.CommentResponse;
import com.taskmaster.collaboration.domain.model.TaskComment;
import com.taskmaster.user.application.mapper.UserMapper;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CommentMapper {

    private final UserMapper userMapper;

    public CommentMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public CommentResponse toResponse(TaskComment comment) {
        if (comment == null) {
            return null;
        }

        List<CommentResponse> replyResponses = comment.getReplies() != null
            ? comment.getReplies().stream().map(this::toResponse).toList()
            : List.of();

        return new CommentResponse(
            comment.getId(),
            comment.getTask().getId(),
            userMapper.toResponse(comment.getAuthor()),
            comment.getParentComment() != null ? comment.getParentComment().getId() : null,
            comment.getContent(),
            comment.isDeleted(),
            replyResponses,
            comment.getCreatedAt(),
            comment.getUpdatedAt()
        );
    }
}
