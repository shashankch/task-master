package com.taskmaster.notification.adapter.out;

import com.taskmaster.notification.domain.port.RealTimeNotificationPublisher;
import com.taskmaster.shared.constant.WebSocketConstants;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class WebSocketNotificationPublisher implements RealTimeNotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(WebSocketNotificationPublisher.class);

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketNotificationPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void sendNotificationToUser(UUID recipientId, Object payload) {
        if (recipientId == null || payload == null) {
            return;
        }

        try {
            messagingTemplate.convertAndSendToUser(
                recipientId.toString(),
                WebSocketConstants.NOTIFICATIONS_QUEUE_DESTINATION,
                payload
            );
            log.debug("Dispatched real-time notification to user: {}", recipientId);
        } catch (Exception e) {
            log.warn("Failed to deliver real-time WebSocket notification to user {}: {}", recipientId, e.getMessage());
        }
    }
}
