package com.taskmaster.shared.constant;

/**
 * Constants related to WebSocket STOMP broker routing, endpoints, and destinations.
 */
public final class WebSocketConstants {

    private WebSocketConstants() {
        // Utility class
    }

    public static final String WS_ENDPOINT = "/ws";
    public static final String APP_DESTINATION_PREFIX = "/app";
    public static final String USER_DESTINATION_PREFIX = "/user";
    public static final String TOPIC_DESTINATION_PREFIX = "/topic";
    public static final String QUEUE_DESTINATION_PREFIX = "/queue";
    public static final String NOTIFICATIONS_QUEUE_DESTINATION = "/queue/notifications";
}
