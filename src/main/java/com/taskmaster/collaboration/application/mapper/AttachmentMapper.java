package com.taskmaster.collaboration.application.mapper;

import com.taskmaster.collaboration.application.dto.AttachmentResponse;
import com.taskmaster.collaboration.domain.model.TaskAttachment;
import com.taskmaster.collaboration.domain.port.FileStorageService;
import com.taskmaster.user.application.mapper.UserMapper;
import org.springframework.stereotype.Component;

@Component
public class AttachmentMapper {

    private final UserMapper userMapper;
    private final FileStorageService fileStorageService;

    public AttachmentMapper(UserMapper userMapper, FileStorageService fileStorageService) {
        this.userMapper = userMapper;
        this.fileStorageService = fileStorageService;
    }

    public AttachmentResponse toResponse(TaskAttachment attachment) {
        if (attachment == null) {
            return null;
        }

        String downloadUrl = fileStorageService.generatePresignedDownloadUrl(
            attachment.getStorageKey(),
            attachment.getFileName()
        );

        return new AttachmentResponse(
            attachment.getId(),
            attachment.getTask().getId(),
            userMapper.toResponse(attachment.getUploader()),
            attachment.getFileName(),
            attachment.getFileSize(),
            attachment.getContentType(),
            downloadUrl,
            attachment.getCreatedAt()
        );
    }
}
