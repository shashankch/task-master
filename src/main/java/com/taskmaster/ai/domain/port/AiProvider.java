package com.taskmaster.ai.domain.port;

public interface AiProvider {

    String generateText(String systemPrompt, String userPrompt);
}
