package com.taskmaster.task.adapter.in;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmaster.shared.config.SecurityConfig;
import com.taskmaster.shared.dto.PageResponse;
import com.taskmaster.shared.exception.GlobalExceptionHandler;
import com.taskmaster.shared.security.JwtConfig;
import com.taskmaster.shared.security.RateLimiterService;
import com.taskmaster.task.application.dto.AssignTaskRequest;
import com.taskmaster.task.application.dto.CreateTaskRequest;
import com.taskmaster.task.application.dto.TaskFilterCriteria;
import com.taskmaster.task.application.dto.TaskResponse;
import com.taskmaster.task.application.dto.UpdateTaskRequest;
import com.taskmaster.task.application.dto.UpdateTaskStatusRequest;
import com.taskmaster.task.application.service.TaskService;
import com.taskmaster.task.domain.model.TaskPriority;
import com.taskmaster.task.domain.model.TaskStatus;
import com.taskmaster.user.application.dto.UserResponse;
import com.taskmaster.user.domain.model.Role;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TaskController.class)
@Import({SecurityConfig.class, JwtConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private RateLimiterService rateLimiterService;

    @Test
    @DisplayName("POST /api/v1/tasks should return 201 when authenticated and valid")
    void createTask_WhenValid_ShouldReturn201() throws Exception {
        UUID creatorId = UUID.randomUUID();
        CreateTaskRequest request = new CreateTaskRequest(
            "Sample Task",
            "Details",
            TaskPriority.HIGH,
            null,
            null,
            null,
            Set.of("dev")
        );

        UserResponse creatorResp = new UserResponse(
            creatorId,
            "c@e.com",
            "creator",
            "Creator",
            null,
            Role.USER,
            true,
            Instant.now(),
            Instant.now()
        );
        TaskResponse response = new TaskResponse(
            UUID.randomUUID(),
            "Sample Task",
            "Details",
            TaskStatus.OPEN,
            TaskPriority.HIGH,
            null,
            creatorResp,
            null,
            null,
            Set.of("dev"),
            0L,
            Instant.now(),
            Instant.now()
        );

        when(taskService.createTask(eq(creatorId), any(CreateTaskRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/tasks")
                .with(jwt().jwt(builder -> builder.subject(creatorId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.title").value("Sample Task"))
            .andExpect(jsonPath("$.data.status").value("OPEN"))
            .andExpect(jsonPath("$.data.priority").value("HIGH"));
    }

    @Test
    @DisplayName("GET /api/v1/tasks/{id} should return 200 with task details")
    void getTaskById_ShouldReturn200() throws Exception {
        UUID taskId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        UserResponse creatorResp = new UserResponse(
            creatorId,
            "c@e.com",
            "creator",
            "Creator",
            null,
            Role.USER,
            true,
            Instant.now(),
            Instant.now()
        );

        TaskResponse response = new TaskResponse(
            taskId,
            "Existing Task",
            "Desc",
            TaskStatus.OPEN,
            TaskPriority.MEDIUM,
            null,
            creatorResp,
            null,
            null,
            Set.of(),
            0L,
            Instant.now(),
            Instant.now()
        );

        when(taskService.getTaskById(taskId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/tasks/" + taskId)
                .with(jwt().jwt(builder -> builder.subject(creatorId.toString()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(taskId.toString()))
            .andExpect(jsonPath("$.data.title").value("Existing Task"));
    }

    @Test
    @DisplayName("GET /api/v1/tasks should return paginated list of tasks")
    void searchTasks_ShouldReturnPaginatedTasks() throws Exception {
        UUID creatorId = UUID.randomUUID();
        UserResponse creatorResp = new UserResponse(
            creatorId,
            "c@e.com",
            "creator",
            "Creator",
            null,
            Role.USER,
            true,
            Instant.now(),
            Instant.now()
        );
        TaskResponse response = new TaskResponse(
            UUID.randomUUID(),
            "Task 1",
            "Desc",
            TaskStatus.OPEN,
            TaskPriority.MEDIUM,
            null,
            creatorResp,
            null,
            null,
            Set.of(),
            0L,
            Instant.now(),
            Instant.now()
        );

        PageResponse<TaskResponse> pageResponse = new PageResponse<>(
            List.of(response),
            0,
            20,
            1L,
            1,
            true,
            true,
            false,
            false
        );

        when(taskService.searchTasks(any(TaskFilterCriteria.class), any(Pageable.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/tasks")
                .with(jwt().jwt(builder -> builder.subject(creatorId.toString())))
                .param("search", "Task")
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[0].title").value("Task 1"))
            .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("PATCH /api/v1/tasks/{id}/status should update status")
    void updateTaskStatus_ShouldReturn200() throws Exception {
        UUID taskId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UpdateTaskStatusRequest request = new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS);

        UserResponse creatorResp = new UserResponse(
            userId,
            "c@e.com",
            "creator",
            "Creator",
            null,
            Role.USER,
            true,
            Instant.now(),
            Instant.now()
        );
        TaskResponse response = new TaskResponse(
            taskId,
            "Task Title",
            "Desc",
            TaskStatus.IN_PROGRESS,
            TaskPriority.MEDIUM,
            null,
            creatorResp,
            null,
            null,
            Set.of(),
            1L,
            Instant.now(),
            Instant.now()
        );

        when(taskService.updateTaskStatus(eq(taskId), eq(userId), any(UpdateTaskStatusRequest.class)))
            .thenReturn(response);

        mockMvc.perform(patch("/api/v1/tasks/" + taskId + "/status")
                .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("DELETE /api/v1/tasks/{id} should soft delete task")
    void deleteTask_ShouldReturn200() throws Exception {
        UUID taskId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/tasks/" + taskId)
                .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.message").value("Task successfully deleted"));
    }
}
