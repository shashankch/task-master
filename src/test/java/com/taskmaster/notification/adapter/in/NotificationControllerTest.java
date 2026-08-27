package com.taskmaster.notification.adapter.in;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.taskmaster.notification.application.dto.NotificationResponse;
import com.taskmaster.notification.application.dto.UnreadCountResponse;
import com.taskmaster.notification.application.service.NotificationService;
import com.taskmaster.notification.domain.model.NotificationType;
import com.taskmaster.shared.config.JacksonConfig;
import com.taskmaster.shared.config.SecurityConfig;
import com.taskmaster.shared.dto.PageResponse;
import com.taskmaster.shared.exception.GlobalExceptionHandler;
import com.taskmaster.shared.security.JwtConfig;
import com.taskmaster.shared.security.RateLimiterService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NotificationController.class)
@Import({SecurityConfig.class, JwtConfig.class, GlobalExceptionHandler.class, JacksonConfig.class})
@ActiveProfiles("test")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private RateLimiterService rateLimiterService;

    @Test
    @DisplayName("GET /api/v1/notifications should return paginated list of notifications")
    void getUserNotifications_ShouldReturn200() throws Exception {
        UUID userId = UUID.randomUUID();
        NotificationResponse response = new NotificationResponse(
            UUID.randomUUID(),
            userId,
            NotificationType.TASK_ASSIGNED,
            "Task Assigned",
            "You were assigned to task",
            "{}",
            false,
            Instant.now(),
            null
        );

        PageResponse<NotificationResponse> page = PageResponse.from(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1));
        when(notificationService.getUserNotifications(eq(userId), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/notifications")
                .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[0].title").value("Task Assigned"));
    }

    @Test
    @DisplayName("GET /api/v1/notifications/unread-count should return unread badge counter")
    void getUnreadCount_ShouldReturnCount() throws Exception {
        UUID userId = UUID.randomUUID();
        when(notificationService.getUnreadCount(userId)).thenReturn(new UnreadCountResponse(5L));

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.unreadCount").value(5));
    }

    @Test
    @DisplayName("PATCH /api/v1/notifications/{id}/read should mark notification read")
    void markAsRead_ShouldReturn200() throws Exception {
        UUID notificationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        NotificationResponse response = new NotificationResponse(
            notificationId,
            userId,
            NotificationType.TASK_ASSIGNED,
            "Task Assigned",
            "Message",
            "{}",
            true,
            Instant.now(),
            Instant.now()
        );

        when(notificationService.markAsRead(notificationId, userId)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/notifications/" + notificationId + "/read")
                .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.isRead").value(true));
    }

    @Test
    @DisplayName("PATCH /api/v1/notifications/read-all should mark all notifications read")
    void markAllAsRead_ShouldReturn200() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(patch("/api/v1/notifications/read-all")
                .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.message").value("All notifications marked as read"));
    }
}
