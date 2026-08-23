package com.taskmaster.notification.application.service;

import com.taskmaster.notification.application.dto.NotificationResponse;
import com.taskmaster.notification.application.dto.UnreadCountResponse;
import com.taskmaster.notification.application.mapper.NotificationMapper;
import com.taskmaster.notification.domain.model.Notification;
import com.taskmaster.notification.domain.model.NotificationType;
import com.taskmaster.notification.domain.port.NotificationRepository;
import com.taskmaster.notification.domain.port.RealTimeNotificationPublisher;
import com.taskmaster.shared.dto.PageResponse;
import com.taskmaster.shared.exception.ForbiddenException;
import com.taskmaster.shared.exception.ResourceNotFoundException;
import com.taskmaster.user.domain.model.User;
import com.taskmaster.user.domain.port.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service managing notification persistence, retrieval, and real-time push.
 */
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final RealTimeNotificationPublisher realTimePublisher;
    private final NotificationMapper notificationMapper;

    public NotificationService(
        NotificationRepository notificationRepository,
        UserRepository userRepository,
        RealTimeNotificationPublisher realTimePublisher,
        NotificationMapper notificationMapper
    ) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.realTimePublisher = realTimePublisher;
        this.notificationMapper = notificationMapper;
    }

    @Transactional
    public NotificationResponse createAndSendNotification(
        UUID recipientId,
        NotificationType type,
        String title,
        String message,
        String metadata
    ) {
        User recipient = userRepository.findById(recipientId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", recipientId));

        Notification notification = new Notification(recipient, type, title, message, metadata);
        Notification savedNotification = notificationRepository.save(notification);

        NotificationResponse response = notificationMapper.toResponse(savedNotification);

        // Push real-time event to user's private STOMP queue
        realTimePublisher.sendNotificationToUser(recipientId, response);

        return response;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getUserNotifications(UUID userId, Boolean unreadOnly, Pageable pageable) {
        Page<Notification> page = Boolean.TRUE.equals(unreadOnly)
            ? notificationRepository.findAllByRecipientIdAndIsReadFalse(userId, pageable)
            : notificationRepository.findAllByRecipientId(userId, pageable);

        return PageResponse.from(page.map(notificationMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(UUID userId) {
        long unreadCount = notificationRepository.countByRecipientIdAndIsReadFalse(userId);
        return new UnreadCountResponse(unreadCount);
    }

    @Transactional
    public NotificationResponse markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        if (!notification.getRecipient().getId().equals(userId)) {
            throw new ForbiddenException("You can only access your own notifications");
        }

        notification.markAsRead();
        Notification updated = notificationRepository.save(notification);
        return notificationMapper.toResponse(updated);
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsReadForUser(userId, Instant.now());
    }
}
