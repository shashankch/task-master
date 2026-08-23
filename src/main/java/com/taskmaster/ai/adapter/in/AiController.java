package com.taskmaster.ai.adapter.in;

import com.taskmaster.ai.application.dto.DetectDuplicatesRequest;
import com.taskmaster.ai.application.dto.DetectDuplicatesResponse;
import com.taskmaster.ai.application.dto.GenerateDescriptionRequest;
import com.taskmaster.ai.application.dto.GeneratedDescriptionResponse;
import com.taskmaster.ai.application.dto.PriorityRecommendationResponse;
import com.taskmaster.ai.application.dto.SuggestLabelsRequest;
import com.taskmaster.ai.application.dto.SuggestLabelsResponse;
import com.taskmaster.ai.application.dto.SuggestPriorityRequest;
import com.taskmaster.ai.application.dto.SummarizeTaskResponse;
import com.taskmaster.ai.application.service.AiAssistantService;
import com.taskmaster.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
@Tag(name = "AI Assistant", description = "Pluggable Generative AI capabilities for task synthesis and summarization")
@SecurityRequirement(name = "BearerAuth")
public class AiController {

    private final AiAssistantService aiAssistantService;

    public AiController(AiAssistantService aiAssistantService) {
        this.aiAssistantService = aiAssistantService;
    }

    @PostMapping("/generate-description")
    @Operation(summary = "Synthesize detailed task description and acceptance criteria from prompt")
    public ResponseEntity<ApiResponse<GeneratedDescriptionResponse>> generateDescription(
        @Valid @RequestBody GenerateDescriptionRequest request
    ) {
        GeneratedDescriptionResponse response = aiAssistantService.generateDescription(request);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PostMapping("/summarize-task/{taskId}")
    @Operation(summary = "Summarize task description and threaded comments into executive bullet points")
    public ResponseEntity<ApiResponse<SummarizeTaskResponse>> summarizeTask(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("taskId") UUID taskId
    ) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        SummarizeTaskResponse response = aiAssistantService.summarizeTask(taskId, currentUserId);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PostMapping("/suggest-priority")
    @Operation(summary = "Analyze task context and recommend priority level with reasoning")
    public ResponseEntity<ApiResponse<PriorityRecommendationResponse>> suggestPriority(
        @Valid @RequestBody SuggestPriorityRequest request
    ) {
        PriorityRecommendationResponse response = aiAssistantService.suggestPriority(request);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PostMapping("/detect-duplicates")
    @Operation(summary = "Detect duplicate or semantically overlapping tasks in workspace")
    public ResponseEntity<ApiResponse<DetectDuplicatesResponse>> detectDuplicates(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody DetectDuplicatesRequest request
    ) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        DetectDuplicatesResponse response = aiAssistantService.detectDuplicates(request, currentUserId);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PostMapping("/suggest-labels")
    @Operation(summary = "Suggest categorization tags and labels based on task content")
    public ResponseEntity<ApiResponse<SuggestLabelsResponse>> suggestLabels(
        @Valid @RequestBody SuggestLabelsRequest request
    ) {
        SuggestLabelsResponse response = aiAssistantService.suggestLabels(request);
        return ResponseEntity.ok(ApiResponse.of(response));
    }
}
