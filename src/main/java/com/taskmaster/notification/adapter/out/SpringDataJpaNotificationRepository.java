package com.taskmaster.notification.adapter.out;

import com.taskmaster.notification.domain.model.Notification;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataJpaNotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findAllByRecipientIdOrderByCreatedAtDesc(UUID recipientId, Pageable pageable);

    Page<Notification> findAllByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(UUID recipientId, Pageable pageable);

    long countByRecipientIdAndIsReadFalse(UUID recipientId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.recipient.id = :recipientId AND n.isRead = false")
    int markAllAsReadForUser(@Param("recipientId") UUID recipientId, @Param("readAt") Instant readAt);
}
