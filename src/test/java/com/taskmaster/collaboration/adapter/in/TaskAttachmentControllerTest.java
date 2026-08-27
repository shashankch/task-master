package com.taskmaster.collaboration.adapter.in;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.taskmaster.collaboration.application.dto.AttachmentResponse;
import com.taskmaster.collaboration.application.service.TaskAttachmentService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

@WebMvcTest(TaskAttachmentController.class)
@Import({SecurityConfig.class, JwtConfig.class, GlobalExceptionHandler.class, JacksonConfig.class})
@ActiveProfiles("test")
class TaskAttachmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskAttachmentService attachmentService;

    @MockitoBean
    private RateLimiterService rateLimiterService;

    @Test
    @DisplayName("POST /api/v1/tasks/{taskId}/attachments should upload file and return 201")
    void uploadAttachment_WhenValid_ShouldReturn201() throws Exception {
        UUID taskId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "doc.pdf",
            "application/pdf",
            "PDF content".getBytes()
        );

        UserResponse uploaderResp = new UserResponse(
            userId,
            "u@e.com",
            "uploader",
            "Uploader",
            null,
            Role.USER,
            true,
            Instant.now(),
            Instant.now()
        );
        AttachmentResponse response = new AttachmentResponse(
            UUID.randomUUID(),
            taskId,
            uploaderResp,
            "doc.pdf",
            1024L,
            "application/pdf",
            "https://storage.taskmaster.io/doc.pdf",
            Instant.now()
        );

        when(attachmentService.uploadAttachment(eq(taskId), eq(userId), any(MultipartFile.class))).thenReturn(response);

        mockMvc.perform(multipart("/api/v1/tasks/" + taskId + "/attachments")
                .file(file)
                .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.fileName").value("doc.pdf"))
            .andExpect(jsonPath("$.data.downloadUrl").value("https://storage.taskmaster.io/doc.pdf"));
    }

    @Test
    @DisplayName("GET /api/v1/tasks/{taskId}/attachments should return attachments list")
    void getTaskAttachments_ShouldReturnList() throws Exception {
        UUID taskId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        UserResponse uploaderResp = new UserResponse(
            userId,
            "u@e.com",
            "uploader",
            "Uploader",
            null,
            Role.USER,
            true,
            Instant.now(),
            Instant.now()
        );
        AttachmentResponse response = new AttachmentResponse(
            UUID.randomUUID(),
            taskId,
            uploaderResp,
            "doc.pdf",
            1024L,
            "application/pdf",
            "https://storage.taskmaster.io/doc.pdf",
            Instant.now()
        );

        when(attachmentService.getTaskAttachments(taskId, userId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/tasks/" + taskId + "/attachments")
                .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].fileName").value("doc.pdf"));
    }

    @Test
    @DisplayName("DELETE /api/v1/attachments/{id} should delete attachment")
    void deleteAttachment_ShouldReturn200() throws Exception {
        UUID attachmentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/attachments/" + attachmentId)
                .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.message").value("Attachment successfully deleted"));
    }
}
