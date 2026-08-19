package com.taskmaster.shared.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

class AuditAwareImplTest {

    private final AuditAwareImpl auditAware = new AuditAwareImpl();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should return empty when no authentication present")
    void getCurrentAuditor_WhenNoAuth_ShouldReturnEmpty() {
        Optional<UUID> auditor = auditAware.getCurrentAuditor();
        assertThat(auditor).isEmpty();
    }

    @Test
    @DisplayName("Should return user UUID when authenticated via JWT")
    void getCurrentAuditor_WhenJwtAuth_ShouldReturnUserId() {
        UUID userId = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("mock-token")
            .header("alg", "none")
            .subject(userId.toString())
            .build();

        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(jwt, null, null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<UUID> auditor = auditAware.getCurrentAuditor();
        assertThat(auditor).contains(userId);
    }
}
