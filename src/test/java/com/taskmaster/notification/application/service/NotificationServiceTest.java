package com.taskmaster.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.taskmaster.notification.application.dto.NotificationResponse;
import com.taskmaster.notification.application.dto.UnreadCountResponse;
import com.taskmaster.notification.application.mapper.NotificationMapper;
import com.taskmaster.notification.domain.model.Notification;
import com.taskmaster.notification.domain.model.NotificationType;
import com.taskmaster.notification.domain.port.NotificationRepository;
import com.taskmaster.notification.domain.port.RealTimeNotificationPublisher;
import com.taskmaster.shared.dto.PageResponse;
import com.taskmaster.shared.exception.ForbiddenException;
import com.taskmaster.user.domain.model.Role;
import com.taskmaster.user.domain.model.User;
import com.taskmaster.user.domain.port.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RealTimeNotificationPublisher realTimePublisher;

    private NotificationMapper notificationMapper;
    private NotificationService notificationService;

    private User recipient;

    @BeforeEach
    void setUp() {
        notificationMapper = new NotificationMapper();
        notificationService = new NotificationService(
            notificationRepository,
            userRepository,
            realTimePublisher,
            notificationMapper
        );

        recipient = new User("user@example.com", "username", "pw", "User Name", Role.USER);
        recipient.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Should create, save, and dispatch real-time notification")
    void createAndSendNotification_WhenValid_ShouldSaveAndPublish() {
        when(userRepository.findById(recipient.getId())).thenReturn(Optional.of(recipient));

        Notification notification = new Notification(
            recipient,
            NotificationType.TASK_ASSIGNED,
            "Task Assigned",
            "You have been assigned to task A",
            "{}"
        );
        notification.setId(UUID.randomUUID());

        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        NotificationResponse response = notificationService.createAndSendNotification(
            recipient.getId(),
            NotificationType.TASK_ASSIGNED,
            "Task Assigned",
            "You have been assigned to task A",
            "{}"
        );

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("Task Assigned");
        verify(realTimePublisher).sendNotificationToUser(eq(recipient.getId()), any(NotificationResponse.class));
    }

    @Test
    @DisplayName("Should retrieve unread notification count")
    void getUnreadCount_ShouldReturnCount() {
        when(notificationRepository.countByRecipientIdAndIsReadFalse(recipient.getId())).thenReturn(4L);

        UnreadCountResponse response = notificationService.getUnreadCount(recipient.getId());

        assertThat(response.unreadCount()).isEqualTo(4L);
    }

    @Test
    @DisplayName("Should retrieve paginated list of user notifications")
    void getUserNotifications_ShouldReturnPage() {
        Notification notification = new Notification(
            recipient,
            NotificationType.COMMENT_ADDED,
            "Comment Added",
            "New comment",
            "{}"
        );
        notification.setId(UUID.randomUUID());

        when(notificationRepository.findAllByRecipientId(eq(recipient.getId()), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(List.of(notification)));

        PageResponse<NotificationResponse> response = notificationService.getUserNotifications(
            recipient.getId(),
            false,
            PageRequest.of(0, 10)
        );

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).title()).isEqualTo("Comment Added");
    }

    @Test
    @DisplayName("Should mark single notification as read")
    void markAsRead_WhenOwner_ShouldMarkRead() {
        UUID notificationId = UUID.randomUUID();
        Notification notification = new Notification(
            recipient,
            NotificationType.TASK_UPDATED,
            "Status Changed",
            "Moved to REVIEW",
            "{}"
        );
        notification.setId(notificationId);

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        NotificationResponse response = notificationService.markAsRead(notificationId, recipient.getId());

        assertThat(response.isRead()).isTrue();
        assertThat(response.readAt()).isNotNull();
    }

    @Test
    @DisplayName("Should throw ForbiddenException when marking another user's notification")
    void markAsRead_WhenNotOwner_ShouldThrowForbidden() {
        UUID notificationId = UUID.randomUUID();
        Notification notification = new Notification(
            recipient,
            NotificationType.TASK_UPDATED,
            "Status Changed",
            "Moved to REVIEW",
            "{}"
        );
        notification.setId(notificationId);

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        UUID otherUserId = UUID.randomUUID();
        assertThatThrownBy(() -> notificationService.markAsRead(notificationId, otherUserId))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("You can only access your own notifications");
    }

    @Test
    @DisplayName("Should mark all notifications as read")
    void markAllAsRead_ShouldCallRepository() {
        notificationService.markAllAsRead(recipient.getId());

        verify(notificationRepository).markAllAsReadForUser(eq(recipient.getId()), any());
    }
}
