package com.taskmaster.ai.adapter.in;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmaster.ai.application.dto.DetectDuplicatesRequest;
import com.taskmaster.ai.application.dto.DetectDuplicatesResponse;
import com.taskmaster.ai.application.dto.DuplicateMatchResponse;
import com.taskmaster.ai.application.dto.GenerateDescriptionRequest;
import com.taskmaster.ai.application.dto.GeneratedDescriptionResponse;
import com.taskmaster.ai.application.dto.PriorityRecommendationResponse;
import com.taskmaster.ai.application.dto.SuggestLabelsRequest;
import com.taskmaster.ai.application.dto.SuggestLabelsResponse;
import com.taskmaster.ai.application.dto.SuggestPriorityRequest;
import com.taskmaster.ai.application.dto.SummarizeTaskResponse;
import com.taskmaster.ai.application.service.AiAssistantService;
import com.taskmaster.shared.config.JacksonConfig;
import com.taskmaster.shared.config.SecurityConfig;
import com.taskmaster.shared.exception.GlobalExceptionHandler;
import com.taskmaster.shared.security.JwtConfig;
import com.taskmaster.shared.security.RateLimiterService;
import com.taskmaster.task.domain.model.TaskPriority;
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

@WebMvcTest(AiController.class)
@Import({SecurityConfig.class, JwtConfig.class, GlobalExceptionHandler.class, JacksonConfig.class})
@ActiveProfiles("test")
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AiAssistantService aiAssistantService;

    @MockitoBean
    private RateLimiterService rateLimiterService;

    @Test
    @DisplayName("POST /api/v1/ai/generate-description should generate markdown description")
    void generateDescription_ShouldReturn200() throws Exception {
        GenerateDescriptionRequest request = new GenerateDescriptionRequest("Add OAuth2 Login", "Support Google & GitHub");
        GeneratedDescriptionResponse response = new GeneratedDescriptionResponse("Generated markdown", List.of("Criteria 1"));

        when(aiAssistantService.generateDescription(any(GenerateDescriptionRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/ai/generate-description")
                .with(jwt().jwt(builder -> builder.subject(UUID.randomUUID().toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.description").value("Generated markdown"));
    }

    @Test
    @DisplayName("POST /api/v1/ai/summarize-task/{taskId} should summarize task")
    void summarizeTask_ShouldReturn200() throws Exception {
        UUID taskId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        SummarizeTaskResponse response = new SummarizeTaskResponse(
            taskId,
            "Executive summary",
            List.of("Takeaway 1"),
            List.of("Action 1")
        );

        when(aiAssistantService.summarizeTask(eq(taskId), eq(userId))).thenReturn(response);

        mockMvc.perform(post("/api/v1/ai/summarize-task/" + taskId)
                .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.summary").value("Executive summary"));
    }

    @Test
    @DisplayName("POST /api/v1/ai/suggest-priority should recommend priority level")
    void suggestPriority_ShouldReturn200() throws Exception {
        SuggestPriorityRequest request = new SuggestPriorityRequest("Fix Crash on Login", "NPE on null username");
        PriorityRecommendationResponse response = new PriorityRecommendationResponse(
            TaskPriority.URGENT,
            0.95,
            "Critical crash affecting authentication"
        );

        when(aiAssistantService.suggestPriority(any(SuggestPriorityRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/ai/suggest-priority")
                .with(jwt().jwt(builder -> builder.subject(UUID.randomUUID().toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.suggestedPriority").value("URGENT"))
            .andExpect(jsonPath("$.data.confidence").value(0.95));
    }

    @Test
    @DisplayName("POST /api/v1/ai/detect-duplicates should detect duplicate tasks")
    void detectDuplicates_ShouldReturn200() throws Exception {
        UUID userId = UUID.randomUUID();
        DetectDuplicatesRequest request = new DetectDuplicatesRequest("Fix login bug", "NPE", null);
        DetectDuplicatesResponse response = new DetectDuplicatesResponse(
            List.of(new DuplicateMatchResponse(UUID.randomUUID(), "Fix login bug", 0.9, "Match"))
        );

        when(aiAssistantService.detectDuplicates(any(DetectDuplicatesRequest.class), eq(userId))).thenReturn(response);

        mockMvc.perform(post("/api/v1/ai/detect-duplicates")
                .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.duplicates[0].title").value("Fix login bug"));
    }

    @Test
    @DisplayName("POST /api/v1/ai/suggest-labels should return suggested tags")
    void suggestLabels_ShouldReturn200() throws Exception {
        SuggestLabelsRequest request = new SuggestLabelsRequest("Fix latency", "Redis cache tuning");
        SuggestLabelsResponse response = new SuggestLabelsResponse(List.of("performance", "redis"));

        when(aiAssistantService.suggestLabels(any(SuggestLabelsRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/ai/suggest-labels")
                .with(jwt().jwt(builder -> builder.subject(UUID.randomUUID().toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.suggestedLabels[0]").value("performance"));
    }
}
