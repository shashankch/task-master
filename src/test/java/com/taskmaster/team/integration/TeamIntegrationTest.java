package com.taskmaster.team.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmaster.team.application.dto.CreateTeamRequest;
import com.taskmaster.team.application.dto.JoinTeamRequest;
import com.taskmaster.team.application.dto.UpdateMemberRoleRequest;
import com.taskmaster.team.application.dto.UpdateTeamRequest;
import com.taskmaster.team.domain.model.TeamRole;
import com.taskmaster.user.application.dto.LoginRequest;
import com.taskmaster.user.application.dto.RegisterRequest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
class TeamIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String ownerToken;
    private String memberToken;
    private UUID memberUserId;

    @BeforeEach
    void setUp() throws Exception {
        // Register & Login Owner
        String ownerEmail = "owner_" + UUID.randomUUID() + "@example.com";
        String ownerUser = "owner_" + UUID.randomUUID().toString().substring(0, 8);
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequest(ownerEmail, ownerUser, "Password@123", "Owner User"))))
            .andExpect(status().isCreated());

        MvcResult loginOwner = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(ownerUser, "Password@123"))))
            .andExpect(status().isOk())
            .andReturn();
        ownerToken = objectMapper.readTree(loginOwner.getResponse().getContentAsString())
            .path("data").path("accessToken").asText();

        // Register & Login Member
        String memberEmail = "member_" + UUID.randomUUID() + "@example.com";
        String memberUserStr = "member_" + UUID.randomUUID().toString().substring(0, 8);
        MvcResult regMember = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequest(memberEmail, memberUserStr, "Password@123", "Member User"))))
            .andExpect(status().isCreated())
            .andReturn();
        memberUserId = UUID.fromString(objectMapper.readTree(regMember.getResponse().getContentAsString())
            .path("data").path("id").asText());

        MvcResult loginMember = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(memberUserStr, "Password@123"))))
            .andExpect(status().isOk())
            .andReturn();
        memberToken = objectMapper.readTree(loginMember.getResponse().getContentAsString())
            .path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("End-to-End Team Workspace Lifecycle: Create -> Join -> Role Governance -> Delete")
    void fullTeamLifecycle() throws Exception {
        // 1. Create Team Workspace
        CreateTeamRequest createReq = new CreateTeamRequest("Platform Core", "Infrastructure & Foundations");
        MvcResult createResult = mockMvc.perform(post("/api/v1/teams")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.name").value("Platform Core"))
            .andExpect(jsonPath("$.data.memberCount").value(1))
            .andReturn();

        JsonNode createdBody = objectMapper.readTree(createResult.getResponse().getContentAsString());
        UUID teamId = UUID.fromString(createdBody.path("data").path("id").asText());
        String inviteCode = createdBody.path("data").path("inviteCode").asText();

        assertThat(inviteCode).isNotBlank();

        // 2. Member Joins via Invite Code
        mockMvc.perform(post("/api/v1/teams/join")
                .header("Authorization", "Bearer " + memberToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new JoinTeamRequest(inviteCode))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(teamId.toString()))
            .andExpect(jsonPath("$.data.memberCount").value(2));

        // 3. List Members
        mockMvc.perform(get("/api/v1/teams/" + teamId + "/members")
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(2));

        // 4. Owner Promotes Member to ADMIN
        mockMvc.perform(patch("/api/v1/teams/" + teamId + "/members/" + memberUserId + "/role")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateMemberRoleRequest(TeamRole.ADMIN))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.role").value("ADMIN"));

        // 5. Admin Updates Team Details
        mockMvc.perform(put("/api/v1/teams/" + teamId)
                .header("Authorization", "Bearer " + memberToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateTeamRequest("Platform Engineering", "Updated desc"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("Platform Engineering"));

        // 6. Delete Team Workspace
        mockMvc.perform(delete("/api/v1/teams/" + teamId)
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.message").value("Team successfully deleted"));
    }
}
