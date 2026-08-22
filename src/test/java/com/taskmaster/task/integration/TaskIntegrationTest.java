package com.taskmaster.task.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmaster.task.application.dto.AssignTaskRequest;
import com.taskmaster.task.application.dto.CreateTaskRequest;
import com.taskmaster.task.application.dto.UpdateTaskRequest;
import com.taskmaster.task.application.dto.UpdateTaskStatusRequest;
import com.taskmaster.task.domain.model.TaskPriority;
import com.taskmaster.task.domain.model.TaskStatus;
import com.taskmaster.user.application.dto.LoginRequest;
import com.taskmaster.user.application.dto.RegisterRequest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TaskIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String userToken;
    private String secondUserToken;
    private UUID secondUserId;

    @BeforeEach
    void setUp() throws Exception {
        // Register & Login User 1
        String email1 = "task_user_" + UUID.randomUUID() + "@example.com";
        String username1 = "task_user_" + UUID.randomUUID().toString().substring(0, 8);
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequest(email1, username1, "Password@123", "Task User 1"))))
            .andExpect(status().isCreated());

        MvcResult login1 = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(username1, "Password@123"))))
            .andExpect(status().isOk())
            .andReturn();

        userToken = objectMapper.readTree(login1.getResponse().getContentAsString())
            .path("data").path("accessToken").asText();

        // Register & Login User 2
        String email2 = "task_user2_" + UUID.randomUUID() + "@example.com";
        String username2 = "task_user2_" + UUID.randomUUID().toString().substring(0, 8);
        MvcResult reg2 = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequest(email2, username2, "Password@123", "Task User 2"))))
            .andExpect(status().isCreated())
            .andReturn();

        secondUserId = UUID.fromString(objectMapper.readTree(reg2.getResponse().getContentAsString())
            .path("data").path("id").asText());

        MvcResult login2 = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(username2, "Password@123"))))
            .andExpect(status().isOk())
            .andReturn();

        secondUserToken = objectMapper.readTree(login2.getResponse().getContentAsString())
            .path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("End-to-End Task Lifecycle: Create -> Filter -> Transition -> Assign -> Soft Delete")
    void fullTaskLifecycle() throws Exception {
        // 1. Create Task
        CreateTaskRequest createReq = new CreateTaskRequest(
            "Implement Distributed Caching",
            "Integrate Redis multi-tier caching with TTLs",
            TaskPriority.HIGH,
            Instant.now().plus(7, ChronoUnit.DAYS),
            null,
            null,
            Set.of("backend", "performance", "redis")
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/tasks")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.title").value("Implement Distributed Caching"))
            .andExpect(jsonPath("$.data.status").value("OPEN"))
            .andExpect(jsonPath("$.data.priority").value("HIGH"))
            .andReturn();

        JsonNode createdBody = objectMapper.readTree(createResult.getResponse().getContentAsString());
        UUID taskId = UUID.fromString(createdBody.path("data").path("id").asText());

        // 2. Fetch by ID
        mockMvc.perform(get("/api/v1/tasks/" + taskId)
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(taskId.toString()))
            .andExpect(jsonPath("$.data.title").value("Implement Distributed Caching"));

        // 3. Search by Keyword
        mockMvc.perform(get("/api/v1/tasks")
                .header("Authorization", "Bearer " + userToken)
                .param("search", "Caching")
                .param("status", "OPEN")
                .param("priority", "HIGH"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[0].id").value(taskId.toString()))
            .andExpect(jsonPath("$.data.totalElements").value(1));

        // 4. Update Task Details
        UpdateTaskRequest updateReq = new UpdateTaskRequest(
            "Implement Distributed Redis Caching",
            "Updated caching design with invalidation events",
            TaskPriority.URGENT,
            Instant.now().plus(5, ChronoUnit.DAYS),
            Set.of("backend", "redis", "urgent")
        );

        mockMvc.perform(put("/api/v1/tasks/" + taskId)
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.title").value("Implement Distributed Redis Caching"))
            .andExpect(jsonPath("$.data.priority").value("URGENT"));

        // 5. Valid Lifecycle Status Transitions (OPEN -> IN_PROGRESS -> REVIEW -> COMPLETED)
        mockMvc.perform(patch("/api/v1/tasks/" + taskId + "/status")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        mockMvc.perform(patch("/api/v1/tasks/" + taskId + "/status")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateTaskStatusRequest(TaskStatus.REVIEW))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("REVIEW"));

        mockMvc.perform(patch("/api/v1/tasks/" + taskId + "/status")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateTaskStatusRequest(TaskStatus.COMPLETED))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        // 6. Invalid Status Transition (COMPLETED -> IN_PROGRESS directly should fail)
        mockMvc.perform(patch("/api/v1/tasks/" + taskId + "/status")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Bad Request"));

        // 7. Assign Task to Second User
        mockMvc.perform(patch("/api/v1/tasks/" + taskId + "/assign")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AssignTaskRequest(secondUserId))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.assignee.id").value(secondUserId.toString()));

        // 8. Soft Delete Task
        mockMvc.perform(delete("/api/v1/tasks/" + taskId)
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.message").value("Task successfully deleted"));

        // 9. Verify Soft Deleted Task is excluded from queries
        mockMvc.perform(get("/api/v1/tasks/" + taskId)
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/tasks")
                .header("Authorization", "Bearer " + userToken)
                .param("search", "Distributed"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(0));
    }
}
