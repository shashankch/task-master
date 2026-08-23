package com.taskmaster.collaboration.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmaster.collaboration.application.dto.CreateCommentRequest;
import com.taskmaster.collaboration.application.dto.UpdateCommentRequest;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CollaborationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String userToken;
    private UUID taskId;

    @BeforeEach
    void setUp() throws Exception {
        String email = "collab_user_" + UUID.randomUUID() + "@example.com";
        String username = "collab_" + UUID.randomUUID().toString().substring(0, 8);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequest(email, username, "Password@123", "Collab User"))))
            .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(username, "Password@123"))))
            .andExpect(status().isOk())
            .andReturn();

        userToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
            .path("data").path("accessToken").asText();

        // Create Task
        CreateTaskRequest createReq = new CreateTaskRequest("Architecture Design", "Write RFC", TaskPriority.HIGH, null, null, null, null);
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
    @DisplayName("End-to-End Collaboration: Threaded Comments + File Attachments")
    void fullCollaborationLifecycle() throws Exception {
        // 1. Post Root Comment
        CreateCommentRequest rootCommentReq = new CreateCommentRequest("Initial architecture looks solid", null);
        MvcResult rootResult = mockMvc.perform(post("/api/v1/tasks/" + taskId + "/comments")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rootCommentReq)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.content").value("Initial architecture looks solid"))
            .andReturn();

        UUID rootCommentId = UUID.fromString(objectMapper.readTree(rootResult.getResponse().getContentAsString())
            .path("data").path("id").asText());

        // 2. Post Nested Reply
        CreateCommentRequest replyReq = new CreateCommentRequest("Agreed, proceeding with implementation", rootCommentId);
        mockMvc.perform(post("/api/v1/tasks/" + taskId + "/comments")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(replyReq)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.content").value("Agreed, proceeding with implementation"))
            .andExpect(jsonPath("$.data.parentCommentId").value(rootCommentId.toString()));

        // 3. Get Threaded Comments
        mockMvc.perform(get("/api/v1/tasks/" + taskId + "/comments")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].id").value(rootCommentId.toString()))
            .andExpect(jsonPath("$.data[0].replies[0].content").value("Agreed, proceeding with implementation"));

        // 4. Update Root Comment
        mockMvc.perform(put("/api/v1/comments/" + rootCommentId)
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateCommentRequest("Initial architecture review passed"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content").value("Initial architecture review passed"));

        // 5. Upload File Attachment
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "diagram.png",
            "image/png",
            "fake-png-binary-data".getBytes()
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/v1/tasks/" + taskId + "/attachments")
                .file(file)
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.fileName").value("diagram.png"))
            .andReturn();

        UUID attachmentId = UUID.fromString(objectMapper.readTree(uploadResult.getResponse().getContentAsString())
            .path("data").path("id").asText());

        // 6. List Attachments
        mockMvc.perform(get("/api/v1/tasks/" + taskId + "/attachments")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].id").value(attachmentId.toString()))
            .andExpect(jsonPath("$.data[0].fileName").value("diagram.png"));

        // 7. Delete Attachment
        mockMvc.perform(delete("/api/v1/attachments/" + attachmentId)
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.message").value("Attachment successfully deleted"));
    }
}
