package com.taskmaster.shared.security;

import com.taskmaster.shared.constant.SecurityConstants;
import java.security.Principal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthChannelInterceptor.class);

    private final JwtDecoder jwtDecoder;

    public WebSocketAuthChannelInterceptor(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader(SecurityConstants.AUTH_HEADER);
            String token = null;

            if (authHeader != null && authHeader.startsWith(SecurityConstants.BEARER_PREFIX)) {
                token = authHeader.substring(SecurityConstants.BEARER_PREFIX.length());
            } else {
                token = accessor.getFirstNativeHeader(SecurityConstants.NATIVE_TOKEN_HEADER);
            }

            if (token != null && !token.isBlank()) {
                try {
                    Jwt jwt = jwtDecoder.decode(token);
                    String userId = jwt.getSubject();
                    List<String> roles = jwt.getClaimAsStringList(SecurityConstants.CLAIM_ROLES);

                    List<SimpleGrantedAuthority> authorities = roles != null
                        ? roles.stream().map(role -> new SimpleGrantedAuthority(SecurityConstants.ROLE_PREFIX + role)).toList()
                        : List.of();

                    Principal principal = new UsernamePasswordAuthenticationToken(userId, null, authorities);
                    accessor.setUser(principal);
                    log.debug("Authenticated STOMP session for user: {}", userId);
                } catch (Exception e) {
                    log.warn("STOMP JWT authentication failed: {}", e.getMessage());
                }
            }
        }

        return message;
    }
}

