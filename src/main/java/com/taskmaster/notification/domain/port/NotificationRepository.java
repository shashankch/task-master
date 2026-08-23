package com.taskmaster.notification.domain.port;

import com.taskmaster.notification.domain.model.Notification;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationRepository {

    Notification save(Notification notification);

    Optional<Notification> findById(UUID id);

    Page<Notification> findAllByRecipientId(UUID recipientId, Pageable pageable);

    Page<Notification> findAllByRecipientIdAndIsReadFalse(UUID recipientId, Pageable pageable);

    long countByRecipientIdAndIsReadFalse(UUID recipientId);

    int markAllAsReadForUser(UUID recipientId, Instant readAt);
}
