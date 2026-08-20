package com.taskmaster.user.adapter.in;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.RSAKey;
import com.taskmaster.shared.config.SecurityConfig;
import com.taskmaster.shared.exception.ConflictException;
import com.taskmaster.shared.exception.GlobalExceptionHandler;
import com.taskmaster.shared.security.JwtConfig;
import com.taskmaster.shared.security.RateLimiterService;
import com.taskmaster.user.application.dto.AuthResponse;
import com.taskmaster.user.application.dto.LoginRequest;
import com.taskmaster.user.application.dto.RefreshTokenRequest;
import com.taskmaster.user.application.dto.RegisterRequest;
import com.taskmaster.user.application.dto.UserResponse;
import com.taskmaster.user.application.service.AuthService;
import com.taskmaster.user.domain.model.Role;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private RateLimiterService rateLimiterService;

    @Test
    @DisplayName("POST /api/v1/auth/register should return 201 when valid")
    void register_WhenValid_ShouldReturn201() throws Exception {
        RegisterRequest request = new RegisterRequest("test@example.com", "validuser", "Password@123", "Valid User");
        UserResponse response = new UserResponse(
            UUID.randomUUID(),
            "test@example.com",
            "validuser",
            "Valid User",
            null,
            Role.USER,
            true,
            Instant.now(),
            Instant.now()
        );

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.email").value("test@example.com"))
            .andExpect(jsonPath("$.data.username").value("validuser"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register should return 400 when invalid password")
    void register_WhenWeakPassword_ShouldReturn400() throws Exception {
        RegisterRequest request = new RegisterRequest("test@example.com", "validuser", "weak", "Valid User");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Validation Failed"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register should return 409 when email exists")
    void register_WhenDuplicateEmail_ShouldReturn409() throws Exception {
        RegisterRequest request = new RegisterRequest("test@example.com", "validuser", "Password@123", "Valid User");

        when(authService.register(any(RegisterRequest.class)))
            .thenThrow(new ConflictException("Email 'test@example.com' is already registered"));

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.title").value("Conflict"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login should return 200 with tokens")
    void login_WhenValid_ShouldReturn200() throws Exception {
        LoginRequest request = new LoginRequest("validuser", "Password@123");
        UserResponse userResponse = new UserResponse(
            UUID.randomUUID(),
            "test@example.com",
            "validuser",
            "Valid User",
            null,
            Role.USER,
            true,
            Instant.now(),
            Instant.now()
        );
        AuthResponse authResponse = AuthResponse.of("access-token", "refresh-token", 900L, userResponse);

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").value("access-token"))
            .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
            .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh should return 200 with new tokens")
    void refresh_WhenValid_ShouldReturn200() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");
        UserResponse userResponse = new UserResponse(
            UUID.randomUUID(),
            "test@example.com",
            "validuser",
            "Valid User",
            null,
            Role.USER,
            true,
            Instant.now(),
            Instant.now()
        );
        AuthResponse authResponse = AuthResponse.of("new-access-token", "new-refresh-token", 900L, userResponse);

        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
            .andExpect(jsonPath("$.data.refreshToken").value("new-refresh-token"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/.well-known/jwks.json should return JWKS keys")
    void jwks_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/auth/.well-known/jwks.json"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.keys").isArray());
    }
}
