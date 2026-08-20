package com.taskmaster.user.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmaster.user.application.dto.LoginRequest;
import com.taskmaster.user.application.dto.RefreshTokenRequest;
import com.taskmaster.user.application.dto.RegisterRequest;
import com.taskmaster.user.application.dto.UpdateProfileRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("End-to-End Auth Lifecycle: Register -> Login -> Profile -> Refresh -> Logout")
    void fullAuthLifecycle() throws Exception {
        // 1. Register
        RegisterRequest registerReq = new RegisterRequest(
            "flow_user@example.com",
            "flow_user",
            "Password@123",
            "Flow User"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerReq)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.email").value("flow_user@example.com"))
            .andExpect(jsonPath("$.data.username").value("flow_user"));

        // 2. Login
        LoginRequest loginReq = new LoginRequest("flow_user", "Password@123");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
            .andReturn();

        JsonNode loginBody = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = loginBody.path("data").path("accessToken").asText();
        String refreshToken = loginBody.path("data").path("refreshToken").asText();

        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();

        // 3. Access Profile with Access Token
        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.email").value("flow_user@example.com"))
            .andExpect(jsonPath("$.data.displayName").value("Flow User"));

        // 4. Update Profile
        UpdateProfileRequest updateReq = new UpdateProfileRequest("Updated Flow User", "https://example.com/avatar.jpg");
        mockMvc.perform(put("/api/v1/users/me")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.displayName").value("Updated Flow User"))
            .andExpect(jsonPath("$.data.avatarUrl").value("https://example.com/avatar.jpg"));

        // 5. Refresh Token Rotation
        RefreshTokenRequest refreshReq = new RefreshTokenRequest(refreshToken);
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
            .andReturn();

        JsonNode refreshBody = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        String newAccessToken = refreshBody.path("data").path("accessToken").asText();
        String newRefreshToken = refreshBody.path("data").path("refreshToken").asText();

        assertThat(newRefreshToken).isNotEqualTo(refreshToken);

        // 6. Access Profile with New Access Token
        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer " + newAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.displayName").value("Updated Flow User"));

        // 7. Replay of Old Revoked Refresh Token should trigger theft detection
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshReq)))
            .andExpect(status().isUnauthorized());

        // 8. Logout with the active refresh token
        RefreshTokenRequest logoutReq = new RefreshTokenRequest(newRefreshToken);
        mockMvc.perform(post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(logoutReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.message").value("Successfully logged out"));
    }
}
