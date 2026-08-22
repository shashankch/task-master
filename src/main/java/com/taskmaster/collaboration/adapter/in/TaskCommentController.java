package com.taskmaster.collaboration.adapter.in;

import com.taskmaster.collaboration.application.dto.CommentResponse;
import com.taskmaster.collaboration.application.dto.CreateCommentRequest;
import com.taskmaster.collaboration.application.dto.UpdateCommentRequest;
import com.taskmaster.collaboration.application.service.TaskCommentService;
import com.taskmaster.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Comments", description = "Hierarchical threaded task comments and discussion endpoints")
@SecurityRequirement(name = "BearerAuth")
public class TaskCommentController {

    private final TaskCommentService commentService;

    public TaskCommentController(TaskCommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/api/v1/tasks/{taskId}/comments")
    @Operation(summary = "Post a comment or reply to a task")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("taskId") UUID taskId,
        @Valid @RequestBody CreateCommentRequest request
    ) {
        UUID authorId = UUID.fromString(jwt.getSubject());
        CommentResponse response = commentService.createComment(taskId, authorId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @GetMapping("/api/v1/tasks/{taskId}/comments")
    @Operation(summary = "Get all hierarchical threaded comments for a task")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getTaskComments(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("taskId") UUID taskId
    ) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        List<CommentResponse> response = commentService.getTaskComments(taskId, currentUserId);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PutMapping("/api/v1/comments/{id}")
    @Operation(summary = "Edit comment content (Author only)")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("id") UUID id,
        @Valid @RequestBody UpdateCommentRequest request
    ) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        CommentResponse response = commentService.updateComment(id, currentUserId, request);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @DeleteMapping("/api/v1/comments/{id}")
    @Operation(summary = "Soft delete a comment (Author only)")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteComment(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("id") UUID id
    ) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        commentService.deleteComment(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.of(Map.of("message", "Comment successfully deleted")));
    }
}
