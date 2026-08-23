package com.taskmaster.notification.adapter.in;

import com.taskmaster.notification.application.dto.NotificationResponse;
import com.taskmaster.notification.application.dto.UnreadCountResponse;
import com.taskmaster.notification.application.service.NotificationService;
import com.taskmaster.shared.dto.ApiResponse;
import com.taskmaster.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Real-time notifications and notification center endpoints")
@SecurityRequirement(name = "BearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "Get current user's notifications (paginated, filterable by unread)")
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getUserNotifications(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(name = "unreadOnly", required = false) Boolean unreadOnly,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        PageResponse<NotificationResponse> response = notificationService.getUserNotifications(userId, unreadOnly, pageable);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get count of unread notifications for badge counter")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        UnreadCountResponse response = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark a single notification as read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("id") UUID id
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        NotificationResponse response = notificationService.markAsRead(id, userId);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all notifications as read for current user")
    public ResponseEntity<ApiResponse<Map<String, String>>> markAllAsRead(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.of(Map.of("message", "All notifications marked as read")));
    }
}
