package com.taskmaster.notification.domain.port;

import java.util.UUID;

public interface RealTimeNotificationPublisher {

    void sendNotificationToUser(UUID recipientId, Object payload);
}
