package com.taskmaster.collaboration.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.taskmaster.collaboration.application.dto.AttachmentResponse;
import com.taskmaster.collaboration.application.mapper.AttachmentMapper;
import com.taskmaster.collaboration.domain.event.TaskAttachmentDeletedEvent;
import com.taskmaster.collaboration.domain.event.TaskAttachmentUploadedEvent;
import com.taskmaster.collaboration.domain.model.TaskAttachment;
import com.taskmaster.collaboration.domain.port.CollaborationEventPublisher;
import com.taskmaster.collaboration.domain.port.FileStorageService;
import com.taskmaster.collaboration.domain.port.TaskAttachmentRepository;
import com.taskmaster.shared.exception.BadRequestException;
import com.taskmaster.task.domain.model.Task;
import com.taskmaster.task.domain.port.TaskRepository;
import com.taskmaster.team.domain.port.TeamMemberRepository;
import com.taskmaster.user.application.mapper.UserMapper;
import com.taskmaster.user.domain.model.Role;
import com.taskmaster.user.domain.model.User;
import com.taskmaster.user.domain.port.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class TaskAttachmentServiceTest {

    @Mock
    private TaskAttachmentRepository taskAttachmentRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private CollaborationEventPublisher eventPublisher;

    private AttachmentMapper attachmentMapper;
    private TaskAttachmentService attachmentService;

    private User uploader;
    private Task task;

    @BeforeEach
    void setUp() {
        UserMapper userMapper = new UserMapper();
        attachmentMapper = new AttachmentMapper(userMapper, fileStorageService);
        attachmentService = new TaskAttachmentService(
            taskAttachmentRepository,
            taskRepository,
            userRepository,
            teamMemberRepository,
            fileStorageService,
            attachmentMapper,
            eventPublisher
        );

        uploader = new User("uploader@example.com", "uploader", "pw", "Uploader", Role.USER);
        uploader.setId(UUID.randomUUID());

        task = new Task("Task Title", "Desc", null, null, uploader, null, null, null);
        task.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Should upload file attachment and publish domain event")
    void uploadAttachment_WhenValid_ShouldSaveAndPublish() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "architecture.pdf",
            "application/pdf",
            "PDF content bytes".getBytes()
        );

        when(taskRepository.findByIdAndNotDeleted(task.getId())).thenReturn(Optional.of(task));
        when(userRepository.findById(uploader.getId())).thenReturn(Optional.of(uploader));
        when(fileStorageService.generatePresignedDownloadUrl(any(), eq("architecture.pdf")))
            .thenReturn("https://storage.taskmaster.io/presigned-url");

        TaskAttachment savedAttachment = new TaskAttachment(
            task,
            uploader,
            "architecture.pdf",
            file.getSize(),
            "application/pdf",
            "tasks/key.pdf"
        );
        savedAttachment.setId(UUID.randomUUID());

        when(taskAttachmentRepository.save(any(TaskAttachment.class))).thenReturn(savedAttachment);

        AttachmentResponse response = attachmentService.uploadAttachment(task.getId(), uploader.getId(), file);

        assertThat(response).isNotNull();
        assertThat(response.fileName()).isEqualTo("architecture.pdf");
        assertThat(response.downloadUrl()).isEqualTo("https://storage.taskmaster.io/presigned-url");

        verify(fileStorageService).uploadFile(any(), any(), eq("application/pdf"));
        verify(eventPublisher).publish(any(TaskAttachmentUploadedEvent.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when uploaded file is empty")
    void uploadAttachment_WhenEmptyFile_ShouldThrowBadRequest() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        assertThatThrownBy(() -> attachmentService.uploadAttachment(task.getId(), uploader.getId(), emptyFile))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("Uploaded file must not be empty");
    }

    @Test
    @DisplayName("Should delete attachment from storage and database")
    void deleteAttachment_WhenUploader_ShouldDelete() {
        UUID attachmentId = UUID.randomUUID();
        TaskAttachment attachment = new TaskAttachment(
            task,
            uploader,
            "architecture.pdf",
            1024L,
            "application/pdf",
            "tasks/key.pdf"
        );
        attachment.setId(attachmentId);

        when(taskAttachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));

        attachmentService.deleteAttachment(attachmentId, uploader.getId());

        verify(fileStorageService).deleteFile("tasks/key.pdf");
        verify(taskAttachmentRepository).delete(attachment);
        verify(eventPublisher).publish(any(TaskAttachmentDeletedEvent.class));
    }
}
