package com.taskmaster.ai.adapter.out;

import com.taskmaster.ai.domain.port.AiProvider;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Universal, vendor-agnostic AI provider using the OpenAI-Compatible Chat Completions standard.
 * Configured with connection and read timeouts and resilient heuristic fallback.
 */
@Component
public class PluggableAiProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(PluggableAiProvider.class);

    private final boolean enabled;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final double temperature;
    private final int timeoutSeconds;
    private final boolean fallbackEnabled;
    private final RestClient restClient;

    public PluggableAiProvider(
        @Value("${app.ai.enabled:true}") boolean enabled,
        @Value("${app.ai.base-url:https://api.groq.com/openai/v1}") String baseUrl,
        @Value("${app.ai.api-key:}") String apiKey,
        @Value("${app.ai.model:llama-3.3-70b-versatile}") String model,
        @Value("${app.ai.temperature:0.2}") double temperature,
        @Value("${app.ai.timeout-seconds:10}") int timeoutSeconds,
        @Value("${app.ai.fallback-enabled:true}") boolean fallbackEnabled
    ) {
        this.enabled = enabled;
        this.baseUrl = baseUrl != null && !baseUrl.isBlank() ? baseUrl : "https://api.groq.com/openai/v1";
        this.apiKey = apiKey;
        this.model = model != null && !model.isBlank() ? model : "llama-3.3-70b-versatile";
        this.temperature = temperature;
        this.timeoutSeconds = timeoutSeconds;
        this.fallbackEnabled = fallbackEnabled;

        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder()
            .requestFactory(requestFactory)
            .build();
    }

    @Override
    public String generateText(String systemPrompt, String userPrompt) {
        if (enabled && apiKey != null && !apiKey.isBlank()) {
            try {
                return callOpenAiCompatibleEndpoint(systemPrompt, userPrompt);
            } catch (Exception e) {
                log.warn("Universal AI endpoint call failed [{}]: {}. Engaging fallback engine.", baseUrl, e.getMessage());
            }
        }

        if (fallbackEnabled) {
            return generateHeuristicResponse(systemPrompt, userPrompt);
        }

        throw new IllegalStateException("AI service unavailable and fallback is disabled");
    }

    private String callOpenAiCompatibleEndpoint(String systemPrompt, String userPrompt) {
        String endpointUrl = baseUrl.endsWith("/chat/completions") ? baseUrl : baseUrl + "/chat/completions";

        Map<String, Object> requestBody = Map.of(
            "model", model,
            "temperature", temperature,
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt != null ? systemPrompt : ""),
                Map.of("role", "user", "content", userPrompt != null ? userPrompt : "")
            )
        );

        var requestSpec = restClient.post()
            .uri(endpointUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBody);

        if (apiKey != null && !apiKey.isBlank()) {
            requestSpec.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        }

        Map<?, ?> response = requestSpec
            .retrieve()
            .body(Map.class);

        return extractTextFromResponse(response);
    }

    private String extractTextFromResponse(Map<?, ?> response) {
        if (response == null) {
            return "";
        }
        try {
            var choices = (List<?>) response.get("choices");
            if (choices != null && !choices.isEmpty()) {
                var first = (Map<?, ?>) choices.get(0);
                var message = (Map<?, ?>) first.get("message");
                if (message != null && message.get("content") != null) {
                    return message.get("content").toString();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse standard OpenAI-compatible response: {}", e.getMessage());
        }
        return "";
    }

    private String generateHeuristicResponse(String systemPrompt, String userPrompt) {
        String combined = (systemPrompt != null ? systemPrompt : "") + " " + (userPrompt != null ? userPrompt : "");
        String promptLower = combined.toLowerCase();

        if (promptLower.contains("priority")) {
            if (promptLower.contains("critical") || promptLower.contains("outage")
                || promptLower.contains("security") || promptLower.contains("crash")) {
                return "{\"priority\": \"URGENT\", \"confidence\": 0.95, "
                    + "\"reasoning\": \"Detected critical terms indicating downtime or security impact.\"}";
            }
            if (promptLower.contains("high") || promptLower.contains("bug")
                || promptLower.contains("performance") || promptLower.contains("deadline")) {
                return "{\"priority\": \"HIGH\", \"confidence\": 0.88, "
                    + "\"reasoning\": \"Identified significant defect, bottleneck, or time-sensitive task.\"}";
            }
            if (promptLower.contains("doc") || promptLower.contains("refactor")
                || promptLower.contains("cleanup") || promptLower.contains("minor")) {
                return "{\"priority\": \"LOW\", \"confidence\": 0.85, "
                    + "\"reasoning\": \"Classified as non-blocking maintenance or routine cleanup.\"}";
            }
            return "{\"priority\": \"MEDIUM\", \"confidence\": 0.80, "
                + "\"reasoning\": \"Standard feature delivery and regular roadmap milestone.\"}";
        }

        if (promptLower.contains("summary") || promptLower.contains("summarize")) {
            return """
                ### Executive Summary
                - Core objective: Streamline platform operations and ensure robust delivery.
                - Status: In progress with alignment across contributors.
                
                ### Key Takeaways
                1. Technical design reviewed and verified.
                2. Automated tests and validation criteria passing.
                
                ### Action Items
                - Complete pending integrations and documentation updates.
                """.stripIndent();
        }

        if (promptLower.contains("label") || promptLower.contains("tag")) {
            return "[\"backend\", \"performance\", \"enhancement\", \"api\"]";
        }

        // Description generator heuristic
        return """
            ### Objective
            Implement the required functionality to enhance system capabilities.
            
            ### Scope & Requirements
            - Design robust domain abstractions following clean architecture.
            - Expose secure, versioned REST endpoints with input validation.
            - Include automated unit and integration tests.
            
            ### Acceptance Criteria
            - [ ] Core business rules validated against test suite
            - [ ] Error responses comply with RFC 7807 ProblemDetail format
            - [ ] Zero linting or Checkstyle regressions
            """.stripIndent();
    }
}
