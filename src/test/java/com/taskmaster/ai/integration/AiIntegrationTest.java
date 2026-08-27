package com.taskmaster.ai.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmaster.ai.application.dto.GenerateDescriptionRequest;
import com.taskmaster.ai.application.dto.SuggestLabelsRequest;
import com.taskmaster.ai.application.dto.SuggestPriorityRequest;
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
class AiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String userToken;
    private UUID taskId;

    @BeforeEach
    void setUp() throws Exception {
        String email = "ai_integ_" + UUID.randomUUID() + "@example.com";
        String username = "ai_" + UUID.randomUUID().toString().substring(0, 8);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequest(email, username, "Password@123", "AI User"))))
            .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(username, "Password@123"))))
            .andExpect(status().isOk())
            .andReturn();

        userToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
            .path("data").path("accessToken").asText();

        // Create Task
        CreateTaskRequest createReq = new CreateTaskRequest(
            "Performance Tuning",
            "Redis cache integration",
            TaskPriority.HIGH,
            null,
            null,
            null,
            null
        );
        MvcResult taskResult = mockMvc.perform(post("/api/v1/tasks")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
            .andExpect(status().isCreated())
            .andReturn();

        taskId = UUID.fromString(objectMapper.readTree(taskResult.getResponse().getContentAsString())
            .path("data").path("id").asText());
    }

    @Test
    @DisplayName("End-to-End AI Assistant Features: Synthesis, Priority, Labels, and Summary")
    void fullAiLifecycle() throws Exception {
        // 1. Generate Description
        GenerateDescriptionRequest genReq = new GenerateDescriptionRequest(
            "Add Redis Caching",
            "Support cache invalidation on write"
        );
        mockMvc.perform(post("/api/v1/ai/generate-description")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(genReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.description").isNotEmpty())
            .andExpect(jsonPath("$.data.suggestedAcceptanceCriteria").isArray());

        // 2. Suggest Priority
        SuggestPriorityRequest prioReq = new SuggestPriorityRequest(
            "Critical Security Vulnerability",
            "SQL injection in search"
        );
        mockMvc.perform(post("/api/v1/ai/suggest-priority")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(prioReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.suggestedPriority").value("URGENT"))
            .andExpect(jsonPath("$.data.reasoning").isNotEmpty());

        // 3. Suggest Labels
        SuggestLabelsRequest labelReq = new SuggestLabelsRequest(
            "Database optimization",
            "Index creation"
        );
        mockMvc.perform(post("/api/v1/ai/suggest-labels")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(labelReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.suggestedLabels").isArray());

        // 4. Summarize Task
        mockMvc.perform(post("/api/v1/ai/summarize-task/" + taskId)
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.taskId").value(taskId.toString()))
            .andExpect(jsonPath("$.data.summary").isNotEmpty());
    }
}
