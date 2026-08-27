package com.taskmaster.notification.application.mapper;

import com.taskmaster.notification.application.dto.NotificationResponse;
import com.taskmaster.notification.domain.model.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(Notification notification) {
        if (notification == null) {
            return null;
        }

        return new NotificationResponse(
            notification.getId(),
            notification.getRecipient().getId(),
            notification.getType(),
            notification.getTitle(),
            notification.getMessage(),
            notification.getMetadata(),
            notification.isRead(),
            notification.getCreatedAt(),
            notification.getReadAt()
        );
    }
}
