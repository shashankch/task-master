package com.taskmaster.shared.config;

import com.taskmaster.shared.constant.WebSocketConstants;
import com.taskmaster.shared.security.WebSocketAuthChannelInterceptor;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket and STOMP message broker configuration with JWT channel security.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthChannelInterceptor authChannelInterceptor;
    private final List<String> allowedOrigins;

    public WebSocketConfig(
        WebSocketAuthChannelInterceptor authChannelInterceptor,
        @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173,http://localhost:8080}")
        List<String> allowedOrigins
    ) {
        this.authChannelInterceptor = authChannelInterceptor;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable in-memory broker for topics (broadcast) and queues (user-specific)
        registry.enableSimpleBroker(WebSocketConstants.TOPIC_DESTINATION_PREFIX, WebSocketConstants.QUEUE_DESTINATION_PREFIX);
        registry.setApplicationDestinationPrefixes(WebSocketConstants.APP_DESTINATION_PREFIX);
        registry.setUserDestinationPrefix(WebSocketConstants.USER_DESTINATION_PREFIX);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = allowedOrigins.toArray(new String[0]);

        // WebSocket endpoint with SockJS fallback
        registry.addEndpoint(WebSocketConstants.WS_ENDPOINT)
            .setAllowedOriginPatterns(origins)
            .withSockJS();

        // Native WebSocket endpoint
        registry.addEndpoint(WebSocketConstants.WS_ENDPOINT)
            .setAllowedOriginPatterns(origins);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authChannelInterceptor);
    }
}
