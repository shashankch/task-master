package com.taskmaster.user.adapter.in;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmaster.shared.config.SecurityConfig;
import com.taskmaster.shared.exception.GlobalExceptionHandler;
import com.taskmaster.shared.security.JwtConfig;
import com.taskmaster.shared.security.RateLimiterService;
import com.taskmaster.user.application.dto.UpdateProfileRequest;
import com.taskmaster.user.application.dto.UserResponse;
import com.taskmaster.user.application.service.UserService;
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

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private RateLimiterService rateLimiterService;

    @Test
    @DisplayName("GET /api/v1/users/me should return 401 when unauthenticated")
    void getMe_WhenUnauthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/users/me should return 200 when authenticated")
    void getMe_WhenAuthenticated_ShouldReturnProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponse response = new UserResponse(
            userId,
            "user@example.com",
            "testuser",
            "Test User",
            "https://example.com/avatar.png",
            Role.USER,
            true,
            Instant.now(),
            Instant.now()
        );

        when(userService.getProfile(userId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/me")
                .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(userId.toString()))
            .andExpect(jsonPath("$.data.email").value("user@example.com"))
            .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    @DisplayName("PUT /api/v1/users/me should update profile and return 200")
    void updateMe_WhenAuthenticated_ShouldReturnUpdatedProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        UpdateProfileRequest request = new UpdateProfileRequest("Updated Name", "https://example.com/new.png");
        UserResponse response = new UserResponse(
            userId,
            "user@example.com",
            "testuser",
            "Updated Name",
            "https://example.com/new.png",
            Role.USER,
            true,
            Instant.now(),
            Instant.now()
        );

        when(userService.updateProfile(eq(userId), any(UpdateProfileRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/users/me")
                .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.displayName").value("Updated Name"))
            .andExpect(jsonPath("$.data.avatarUrl").value("https://example.com/new.png"));
    }
}
