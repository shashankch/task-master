package com.taskmaster.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmaster.ai.application.dto.DetectDuplicatesRequest;
import com.taskmaster.ai.application.dto.DetectDuplicatesResponse;
import com.taskmaster.ai.application.dto.GenerateDescriptionRequest;
import com.taskmaster.ai.application.dto.GeneratedDescriptionResponse;
import com.taskmaster.ai.application.dto.PriorityRecommendationResponse;
import com.taskmaster.ai.application.dto.SuggestLabelsRequest;
import com.taskmaster.ai.application.dto.SuggestLabelsResponse;
import com.taskmaster.ai.application.dto.SuggestPriorityRequest;
import com.taskmaster.ai.application.dto.SummarizeTaskResponse;
import com.taskmaster.ai.domain.port.AiProvider;
import com.taskmaster.collaboration.domain.model.TaskComment;
import com.taskmaster.collaboration.domain.port.TaskCommentRepository;
import com.taskmaster.task.domain.model.Task;
import com.taskmaster.task.domain.model.TaskPriority;
import com.taskmaster.task.domain.port.TaskRepository;
import com.taskmaster.team.domain.port.TeamMemberRepository;
import com.taskmaster.user.domain.model.Role;
import com.taskmaster.user.domain.model.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class AiAssistantServiceTest {

    @Mock
    private AiProvider aiProvider;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskCommentRepository taskCommentRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    private ObjectMapper objectMapper;
    private AiAssistantService aiService;

    private User user;
    private Task task;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        aiService = new AiAssistantService(
            aiProvider,
            taskRepository,
            taskCommentRepository,
            teamMemberRepository,
            objectMapper
        );

        user = new User("ai@example.com", "ai_user", "pw", "AI User", Role.USER);
        user.setId(UUID.randomUUID());

        task = new Task("Refactor Auth Engine", "Decouple token service", null, null, user, null, null, null);
        task.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Should generate structured description and acceptance criteria from prompt")
    void generateDescription_ShouldReturnStructuredResponse() {
        String generatedMarkdown = """
            ### Objective
            Implement Redis caching.
            
            ### Acceptance Criteria
            - [ ] Cache hit ratio exceeds 90%
            - [ ] Cache invalidation on mutation
            """;

        when(aiProvider.generateText(any(), any())).thenReturn(generatedMarkdown);

        GenerateDescriptionRequest request = new GenerateDescriptionRequest("Cache Layer", "Add Redis cache");
        GeneratedDescriptionResponse response = aiService.generateDescription(request);

        assertThat(response).isNotNull();
        assertThat(response.description()).contains("Implement Redis caching");
        assertThat(response.suggestedAcceptanceCriteria()).contains("Cache hit ratio exceeds 90%");
    }

    @Test
    @DisplayName("Should summarize task and comments into executive overview")
    void summarizeTask_ShouldReturnSummary() {
        when(taskRepository.findByIdAndNotDeleted(task.getId())).thenReturn(Optional.of(task));

        TaskComment comment = new TaskComment(task, user, null, "Completed baseline schema");
        when(taskCommentRepository.findRootCommentsByTaskId(task.getId())).thenReturn(List.of(comment));
        when(aiProvider.generateText(any(), any())).thenReturn("Executive summary of progress");

        SummarizeTaskResponse response = aiService.summarizeTask(task.getId(), user.getId());

        assertThat(response).isNotNull();
        assertThat(response.taskId()).isEqualTo(task.getId());
        assertThat(response.summary()).isEqualTo("Executive summary of progress");
        assertThat(response.keyTakeaways()).isNotEmpty();
    }

    @Test
    @DisplayName("Should recommend priority level with reasoning")
    void suggestPriority_ShouldReturnPriority() {
        String jsonResponse = "{\"priority\": \"HIGH\", \"confidence\": 0.90, \"reasoning\": \"Performance bottleneck\"}";
        when(aiProvider.generateText(any(), any())).thenReturn(jsonResponse);

        SuggestPriorityRequest request = new SuggestPriorityRequest("Fix DB Connection Leak", "Connections pool exhaust under load");
        PriorityRecommendationResponse response = aiService.suggestPriority(request);

        assertThat(response.suggestedPriority()).isEqualTo(TaskPriority.HIGH);
        assertThat(response.confidence()).isEqualTo(0.90);
        assertThat(response.reasoning()).isEqualTo("Performance bottleneck");
    }

    @Test
    @DisplayName("Should detect semantically similar duplicate tasks")
    void detectDuplicates_ShouldReturnMatches() {
        when(taskRepository.findAll(any(Specification.class), any()))
            .thenReturn(new PageImpl<>(List.of(task)));

        DetectDuplicatesRequest request = new DetectDuplicatesRequest("Refactor Auth Engine", "Token cleanup", null);
        DetectDuplicatesResponse response = aiService.detectDuplicates(request, user.getId());

        assertThat(response.duplicates()).isNotEmpty();
        assertThat(response.duplicates().get(0).title()).isEqualTo("Refactor Auth Engine");
    }

    @Test
    @DisplayName("Should suggest technical labels from task context")
    void suggestLabels_ShouldReturnLabels() {
        when(aiProvider.generateText(any(), any())).thenReturn("[\"backend\", \"redis\", \"performance\"]");

        SuggestLabelsRequest request = new SuggestLabelsRequest("Redis Optimization", "Implement ZSET sliding window");
        SuggestLabelsResponse response = aiService.suggestLabels(request);

        assertThat(response.suggestedLabels()).contains("backend", "redis", "performance");
    }
}
