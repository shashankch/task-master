package com.taskmaster.collaboration.adapter.in;

import com.taskmaster.collaboration.application.dto.AttachmentResponse;
import com.taskmaster.collaboration.application.service.TaskAttachmentService;
import com.taskmaster.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Tag(name = "Attachments", description = "Task file attachments and S3/MinIO upload/download endpoints")
@SecurityRequirement(name = "BearerAuth")
public class TaskAttachmentController {

    private final TaskAttachmentService attachmentService;

    public TaskAttachmentController(TaskAttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping(value = "/api/v1/tasks/{taskId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a file attachment to a task (Max 10MB)")
    public ResponseEntity<ApiResponse<AttachmentResponse>> uploadAttachment(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("taskId") UUID taskId,
        @RequestParam("file") MultipartFile file
    ) {
        UUID uploaderId = UUID.fromString(jwt.getSubject());
        AttachmentResponse response = attachmentService.uploadAttachment(taskId, uploaderId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @GetMapping("/api/v1/tasks/{taskId}/attachments")
    @Operation(summary = "List all file attachments for a task with pre-signed download URLs")
    public ResponseEntity<ApiResponse<List<AttachmentResponse>>> getTaskAttachments(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("taskId") UUID taskId
    ) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        List<AttachmentResponse> response = attachmentService.getTaskAttachments(taskId, currentUserId);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @DeleteMapping("/api/v1/attachments/{id}")
    @Operation(summary = "Delete a file attachment (Uploader only)")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteAttachment(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("id") UUID id
    ) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        attachmentService.deleteAttachment(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.of(Map.of("message", "Attachment successfully deleted")));
    }
}
