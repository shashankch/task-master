package com.taskmaster.shared.config;

import com.taskmaster.shared.constant.WebSocketConstants;
import com.taskmaster.shared.security.WebSocketAuthChannelInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthChannelInterceptor authChannelInterceptor;

    public WebSocketConfig(WebSocketAuthChannelInterceptor authChannelInterceptor) {
        this.authChannelInterceptor = authChannelInterceptor;
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
        // WebSocket endpoint with SockJS fallback
        registry.addEndpoint(WebSocketConstants.WS_ENDPOINT)
            .setAllowedOriginPatterns("*")
            .withSockJS();

        // Native WebSocket endpoint
        registry.addEndpoint(WebSocketConstants.WS_ENDPOINT)
            .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authChannelInterceptor);
    }
}
