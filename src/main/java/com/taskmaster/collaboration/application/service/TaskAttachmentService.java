package com.taskmaster.collaboration.application.service;

import com.taskmaster.collaboration.application.dto.AttachmentResponse;
import com.taskmaster.collaboration.application.mapper.AttachmentMapper;
import com.taskmaster.collaboration.domain.event.TaskAttachmentDeletedEvent;
import com.taskmaster.collaboration.domain.event.TaskAttachmentUploadedEvent;
import com.taskmaster.collaboration.domain.model.TaskAttachment;
import com.taskmaster.collaboration.domain.port.CollaborationEventPublisher;
import com.taskmaster.collaboration.domain.port.FileStorageService;
import com.taskmaster.collaboration.domain.port.TaskAttachmentRepository;
import com.taskmaster.shared.exception.BadRequestException;
import com.taskmaster.shared.exception.ForbiddenException;
import com.taskmaster.shared.exception.ResourceNotFoundException;
import com.taskmaster.task.domain.model.Task;
import com.taskmaster.task.domain.port.TaskRepository;
import com.taskmaster.team.domain.port.TeamMemberRepository;
import com.taskmaster.user.domain.model.User;
import com.taskmaster.user.domain.port.UserRepository;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Application service managing multipart task attachments and S3/MinIO storage.
 */
@Service
public class TaskAttachmentService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L; // 10MB

    private final TaskAttachmentRepository taskAttachmentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final FileStorageService fileStorageService;
    private final AttachmentMapper attachmentMapper;
    private final CollaborationEventPublisher eventPublisher;

    public TaskAttachmentService(
        TaskAttachmentRepository taskAttachmentRepository,
        TaskRepository taskRepository,
        UserRepository userRepository,
        TeamMemberRepository teamMemberRepository,
        FileStorageService fileStorageService,
        AttachmentMapper attachmentMapper,
        CollaborationEventPublisher eventPublisher
    ) {
        this.taskAttachmentRepository = taskAttachmentRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.fileStorageService = fileStorageService;
        this.attachmentMapper = attachmentMapper;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public AttachmentResponse uploadAttachment(UUID taskId, UUID uploaderId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file must not be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds maximum allowed limit of 10MB");
        }

        Task task = taskRepository.findByIdAndNotDeleted(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        if (task.getTeamId() != null && !teamMemberRepository.existsByTeamIdAndUserId(task.getTeamId(), uploaderId)) {
            throw new ForbiddenException("You are not a member of the team this task belongs to");
        }

        User uploader = userRepository.findById(uploaderId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", uploaderId));

        String rawFilename = file.getOriginalFilename();
        String originalFilename = (rawFilename != null && !rawFilename.isBlank())
            ? java.nio.file.Paths.get(rawFilename).getFileName().toString()
            : "attachment";
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        String storageKey = String.format("tasks/%s/%s-%s", taskId, UUID.randomUUID(), originalFilename);

        try {
            fileStorageService.uploadFile(storageKey, file.getBytes(), contentType);
        } catch (IOException e) {
            throw new BadRequestException("Failed to read file content");
        }

        TaskAttachment attachment = new TaskAttachment(
            task,
            uploader,
            originalFilename,
            file.getSize(),
            contentType,
            storageKey
        );

        TaskAttachment savedAttachment = taskAttachmentRepository.save(attachment);

        eventPublisher.publish(TaskAttachmentUploadedEvent.of(
            savedAttachment.getId(),
            task.getId(),
            uploader.getId(),
            savedAttachment.getFileName()
        ));

        return attachmentMapper.toResponse(savedAttachment);
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> getTaskAttachments(UUID taskId, UUID currentUserId) {
        Task task = taskRepository.findByIdAndNotDeleted(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        if (task.getTeamId() != null && !teamMemberRepository.existsByTeamIdAndUserId(task.getTeamId(), currentUserId)) {
            throw new ForbiddenException("You are not a member of the team this task belongs to");
        }

        List<TaskAttachment> attachments = taskAttachmentRepository.findAllByTaskId(taskId);
        return attachments.stream().map(attachmentMapper::toResponse).toList();
    }

    @Transactional
    public void deleteAttachment(UUID attachmentId, UUID currentUserId) {
        TaskAttachment attachment = taskAttachmentRepository.findById(attachmentId)
            .orElseThrow(() -> new ResourceNotFoundException("TaskAttachment", "id", attachmentId));

        if (!attachment.getUploader().getId().equals(currentUserId)) {
            throw new ForbiddenException("You can only delete attachments you uploaded");
        }

        fileStorageService.deleteFile(attachment.getStorageKey());
        taskAttachmentRepository.delete(attachment);

        eventPublisher.publish(TaskAttachmentDeletedEvent.of(attachment.getId(), attachment.getTask().getId(), currentUserId));
    }
}
