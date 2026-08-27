package com.taskmaster.team.adapter.in;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmaster.shared.config.JacksonConfig;
import com.taskmaster.shared.config.SecurityConfig;
import com.taskmaster.shared.exception.GlobalExceptionHandler;
import com.taskmaster.shared.security.JwtConfig;
import com.taskmaster.shared.security.RateLimiterService;
import com.taskmaster.team.application.dto.CreateTeamRequest;
import com.taskmaster.team.application.dto.JoinTeamRequest;
import com.taskmaster.team.application.dto.TeamDetailResponse;
import com.taskmaster.team.application.dto.TeamResponse;
import com.taskmaster.team.application.service.TeamService;
import com.taskmaster.user.application.dto.UserResponse;
import com.taskmaster.user.domain.model.Role;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TeamController.class)
@Import({SecurityConfig.class, JwtConfig.class, GlobalExceptionHandler.class, JacksonConfig.class})
@ActiveProfiles("test")
class TeamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TeamService teamService;

    @MockitoBean
    private RateLimiterService rateLimiterService;

    @Test
    @DisplayName("POST /api/v1/teams should create team and return 201")
    void createTeam_WhenValid_ShouldReturn201() throws Exception {
        UUID ownerId = UUID.randomUUID();
        CreateTeamRequest request = new CreateTeamRequest("Dev Team", "Desc");

        UserResponse ownerResp = new UserResponse(
            ownerId,
            "owner@e.com",
            "owner",
            "Owner",
            null,
            Role.USER,
            true,
            Instant.now(),
            Instant.now()
        );
        TeamResponse response = new TeamResponse(
            UUID.randomUUID(),
            "Dev Team",
            "Desc",
            ownerResp,
            "INVITE123",
            1,
            Instant.now(),
            Instant.now()
        );

        when(teamService.createTeam(eq(ownerId), any(CreateTeamRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/teams")
                .with(jwt().jwt(builder -> builder.subject(ownerId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.name").value("Dev Team"))
            .andExpect(jsonPath("$.data.inviteCode").value("INVITE123"));
    }

    @Test
    @DisplayName("GET /api/v1/teams/{id} should return team details")
    void getTeamById_WhenMember_ShouldReturn200() throws Exception {
        UUID teamId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        UserResponse ownerResp = new UserResponse(
            userId,
            "owner@e.com",
            "owner",
            "Owner",
            null,
            Role.USER,
            true,
            Instant.now(),
            Instant.now()
        );
        TeamDetailResponse response = new TeamDetailResponse(
            teamId,
            "Dev Team",
            "Desc",
            ownerResp,
            "INVITE123",
            List.of(),
            Instant.now(),
            Instant.now()
        );

        when(teamService.getTeamById(teamId, userId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/teams/" + teamId)
                .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(teamId.toString()))
            .andExpect(jsonPath("$.data.name").value("Dev Team"));
    }

    @Test
    @DisplayName("POST /api/v1/teams/join should join team with invite code")
    void joinTeam_WhenValidCode_ShouldReturn200() throws Exception {
        UUID userId = UUID.randomUUID();
        JoinTeamRequest request = new JoinTeamRequest("INVITE123");

        UserResponse ownerResp = new UserResponse(
            UUID.randomUUID(),
            "owner@e.com",
            "owner",
            "Owner",
            null,
            Role.USER,
            true,
            Instant.now(),
            Instant.now()
        );
        TeamResponse response = new TeamResponse(
            UUID.randomUUID(),
            "Dev Team",
            "Desc",
            ownerResp,
            "INVITE123",
            2,
            Instant.now(),
            Instant.now()
        );

        when(teamService.joinTeamByInviteCode(eq(userId), any(JoinTeamRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/teams/join")
                .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.memberCount").value(2));
    }

    @Test
    @DisplayName("DELETE /api/v1/teams/{id} should delete team")
    void deleteTeam_WhenOwner_ShouldReturn200() throws Exception {
        UUID teamId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/teams/" + teamId)
                .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.message").value("Team successfully deleted"));
    }
}
