package com.taskmaster.notification.adapter.out;

import com.taskmaster.notification.domain.model.Notification;
import com.taskmaster.notification.domain.port.NotificationRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class JpaNotificationRepository implements NotificationRepository {

    private final SpringDataJpaNotificationRepository repository;

    public JpaNotificationRepository(SpringDataJpaNotificationRepository repository) {
        this.repository = repository;
    }

    @Override
    public Notification save(Notification notification) {
        return repository.save(notification);
    }

    @Override
    public Optional<Notification> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public Page<Notification> findAllByRecipientId(UUID recipientId, Pageable pageable) {
        return repository.findAllByRecipientIdOrderByCreatedAtDesc(recipientId, pageable);
    }

    @Override
    public Page<Notification> findAllByRecipientIdAndIsReadFalse(UUID recipientId, Pageable pageable) {
        return repository.findAllByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(recipientId, pageable);
    }

    @Override
    public long countByRecipientIdAndIsReadFalse(UUID recipientId) {
        return repository.countByRecipientIdAndIsReadFalse(recipientId);
    }

    @Override
    public int markAllAsReadForUser(UUID recipientId, Instant readAt) {
        return repository.markAllAsReadForUser(recipientId, readAt);
    }
}
