package com.taskmaster.notification.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmaster.collaboration.application.dto.CreateCommentRequest;
import com.taskmaster.task.application.dto.CreateTaskRequest;
import com.taskmaster.task.domain.model.TaskPriority;
import com.taskmaster.user.application.dto.LoginRequest;
import com.taskmaster.user.application.dto.RegisterRequest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
class NotificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String user1Token;
    private UUID user1Id;
    private String user2Token;
    private UUID user2Id;

    @BeforeEach
    void setUp() throws Exception {
        // Register User 1
        String u1Email = "n_u1_" + UUID.randomUUID() + "@example.com";
        String u1Username = "nu1_" + UUID.randomUUID().toString().substring(0, 8);
        MvcResult reg1 = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequest(u1Email, u1Username, "Password@123", "User 1"))))
            .andExpect(status().isCreated())
            .andReturn();
        user1Id = UUID.fromString(objectMapper.readTree(reg1.getResponse().getContentAsString())
            .path("data").path("id").asText());

        MvcResult login1 = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(u1Username, "Password@123"))))
            .andExpect(status().isOk())
            .andReturn();
        user1Token = objectMapper.readTree(login1.getResponse().getContentAsString())
            .path("data").path("accessToken").asText();

        // Register User 2
        String u2Email = "n_u2_" + UUID.randomUUID() + "@example.com";
        String u2Username = "nu2_" + UUID.randomUUID().toString().substring(0, 8);
        MvcResult reg2 = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequest(u2Email, u2Username, "Password@123", "User 2"))))
            .andExpect(status().isCreated())
            .andReturn();
        user2Id = UUID.fromString(objectMapper.readTree(reg2.getResponse().getContentAsString())
            .path("data").path("id").asText());

        MvcResult login2 = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(u2Username, "Password@123"))))
            .andExpect(status().isOk())
            .andReturn();
        user2Token = objectMapper.readTree(login2.getResponse().getContentAsString())
            .path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("End-to-End Notification Flow: Task Assignment -> Notification Delivery -> Read Actions")
    void fullNotificationLifecycle() throws Exception {
        // 1. User 1 creates task assigned to User 2
        CreateTaskRequest createReq = new CreateTaskRequest(
            "Implement Notification Hub",
            "WebSocket and event integration",
            TaskPriority.HIGH,
            null,
            user2Id,
            null,
            null
        );

        MvcResult taskResult = mockMvc.perform(post("/api/v1/tasks")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
            .andExpect(status().isCreated())
            .andReturn();

        UUID taskId = UUID.fromString(objectMapper.readTree(taskResult.getResponse().getContentAsString())
            .path("data").path("id").asText());

        // 2. User 2 checks notifications -> should see TASK_ASSIGNED
        MvcResult notifResult = mockMvc.perform(get("/api/v1/notifications")
                .header("Authorization", "Bearer " + user2Token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[0].type").value("TASK_ASSIGNED"))
            .andExpect(jsonPath("$.data.content[0].isRead").value(false))
            .andReturn();

        JsonNode notifJson = objectMapper.readTree(notifResult.getResponse().getContentAsString());
        UUID notifId = UUID.fromString(notifJson.path("data").path("content").get(0).path("id").asText());

        // 3. User 2 checks unread count
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                .header("Authorization", "Bearer " + user2Token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.unreadCount").value(1));

        // 4. User 2 marks single notification read
        mockMvc.perform(patch("/api/v1/notifications/" + notifId + "/read")
                .header("Authorization", "Bearer " + user2Token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.isRead").value(true));

        // 5. User 2 unread count drops to 0
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                .header("Authorization", "Bearer " + user2Token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.unreadCount").value(0));

        // 6. User 2 comments on the task -> User 1 receives COMMENT_ADDED
        mockMvc.perform(post("/api/v1/tasks/" + taskId + "/comments")
                .header("Authorization", "Bearer " + user2Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateCommentRequest("Starting work now", null))))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/notifications")
                .header("Authorization", "Bearer " + user1Token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[0].type").value("COMMENT_ADDED"));

        // 7. User 1 marks all as read
        mockMvc.perform(patch("/api/v1/notifications/read-all")
                .header("Authorization", "Bearer " + user1Token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.message").value("All notifications marked as read"));
    }
}
