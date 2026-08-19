# ADR 0005: Pluggable AI Provider Strategy via Spring AI Profiles

## Status
Accepted

## Context
Generative AI capabilities (task description synthesis, comment summarization, smart priority detection) should be flexible, cost-effective, and functional across diverse developer environments without locking into a single proprietary paid vendor.

Developers and deployment environments require:
- Free tier cloud options (Google Gemini via AI Studio, Groq free tier)
- Completely offline local inference (Ollama running models such as Llama 3.2)
- Extensibility to paid cloud providers (OpenAI, Anthropic) if configured

## Decision
We leverage **Spring AI**'s unified `ChatClient` abstraction and support multi-provider activation using Spring profiles:

- `ai-gemini` (Default / Free Cloud): Google GenAI starter connecting to Gemini 2.5 Flash via free Google AI Studio API keys.
- `ai-groq` (Alternative / Free Cloud): OpenAI-compatible starter pointing to Groq's high-speed inference endpoints.
- `ai-ollama` (Local / Offline): Local inference without external network calls.
- `ai-openai` (Enterprise / Paid): Standard OpenAI starter.

The application services inject `ChatClient` and remain completely agnostic of the active LLM backend.

## Consequences
### Positive
- Zero mandatory API costs for development and local testing.
- Provider switching accomplished via a single command line argument or environment variable (`--spring.profiles.active=dev,ai-gemini`).
- Consistent prompt template execution and structured JSON output mapping.

### Negative
- Model behavior and latency characteristics vary across providers, requiring robust error handling and timeout fallbacks.
