package com.taskmaster.collaboration.adapter.in;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmaster.collaboration.application.dto.CommentResponse;
import com.taskmaster.collaboration.application.dto.CreateCommentRequest;
import com.taskmaster.collaboration.application.dto.UpdateCommentRequest;
import com.taskmaster.collaboration.application.service.TaskCommentService;
import com.taskmaster.shared.config.JacksonConfig;
import com.taskmaster.shared.config.SecurityConfig;
import com.taskmaster.shared.exception.GlobalExceptionHandler;
import com.taskmaster.shared.security.JwtConfig;
import com.taskmaster.shared.security.RateLimiterService;
import com.taskmaster.user.application.dto.UserResponse;
import com.taskmaster.user.domain.model.Role;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TaskCommentController.class)
@Import({SecurityConfig.class, JwtConfig.class, GlobalExceptionHandler.class, JacksonConfig.class})
@ActiveProfiles("test")
class TaskCommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskCommentService commentService;

    @MockitoBean
    private RateLimiterService rateLimiterService;

    @Test
    @DisplayName("POST /api/v1/tasks/{taskId}/comments should post comment and return 201")
    void createComment_WhenValid_ShouldReturn201() throws Exception {
        UUID taskId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        CreateCommentRequest request = new CreateCommentRequest("Great task progress", null);

        UserResponse authorResp = new UserResponse(
            authorId,
            "a@e.com",
            "author",
            "Author",
            null,
            Role.USER,
            true,
            Instant.now(),
            Instant.now()
        );
        CommentResponse response = new CommentResponse(
            UUID.randomUUID(),
            taskId,
            authorResp,
            null,
            "Great task progress",
            false,
            List.of(),
            Instant.now(),
            Instant.now()
        );

        when(commentService.createComment(eq(taskId), eq(authorId), any(CreateCommentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/tasks/" + taskId + "/comments")
                .with(jwt().jwt(builder -> builder.subject(authorId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.content").value("Great task progress"));
    }

    @Test
    @DisplayName("GET /api/v1/tasks/{taskId}/comments should return list of threaded comments")
    void getTaskComments_ShouldReturnComments() throws Exception {
        UUID taskId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        UserResponse authorResp = new UserResponse(
            userId,
            "a@e.com",
            "author",
            "Author",
            null,
            Role.USER,
            true,
            Instant.now(),
            Instant.now()
        );
        CommentResponse response = new CommentResponse(
            UUID.randomUUID(),
            taskId,
            authorResp,
            null,
            "Root comment",
            false,
            List.of(),
            Instant.now(),
            Instant.now()
        );

        when(commentService.getTaskComments(taskId, userId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/tasks/" + taskId + "/comments")
                .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].content").value("Root comment"));
    }

    @Test
    @DisplayName("PUT /api/v1/comments/{id} should edit comment and return 200")
    void updateComment_ShouldReturn200() throws Exception {
        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UpdateCommentRequest request = new UpdateCommentRequest("Updated comment content");

        UserResponse authorResp = new UserResponse(
            userId,
            "a@e.com",
            "author",
            "Author",
            null,
            Role.USER,
            true,
            Instant.now(),
            Instant.now()
        );
        CommentResponse response = new CommentResponse(
            commentId,
            UUID.randomUUID(),
            authorResp,
            null,
            "Updated comment content",
            false,
            List.of(),
            Instant.now(),
            Instant.now()
        );

        when(commentService.updateComment(eq(commentId), eq(userId), any(UpdateCommentRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/comments/" + commentId)
                .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content").value("Updated comment content"));
    }

    @Test
    @DisplayName("DELETE /api/v1/comments/{id} should soft delete comment")
    void deleteComment_ShouldReturn200() throws Exception {
        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/comments/" + commentId)
                .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.message").value("Comment successfully deleted"));
    }
}
