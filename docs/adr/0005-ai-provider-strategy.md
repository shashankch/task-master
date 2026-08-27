# ADR 0005: Universal OpenAI-Compatible AI Provider & Gateway Strategy

## Status
Accepted

## Context
Generative AI capabilities (task description synthesis, comment thread summarization, priority recommendation, semantic duplicate detection, and label tagging) must be flexible, vendor-agnostic, and cost-effective across diverse local, cloud, and enterprise environments without locking into proprietary vendor SDKs.

In modern enterprise architectures (2026 standard):
- LLM providers (Groq Cloud, Google Gemini, Ollama, OpenAI, vLLM, DeepSeek, Together AI) natively implement the **OpenAI Chat Completions REST Specification** (`POST /v1/chat/completions`).
- Production systems route AI requests through **AI Gateways** (e.g., LiteLLM, Portkey, Cloudflare AI Gateway) for centralized rate limiting, semantic caching, token budgeting, and automatic multi-provider failover.
- Vendor-specific property files and hardcoded vendor branch logic create technical debt and operational fragility.

## Decision
We implement a **Universal OpenAI-Compatible AI Client** backed by a clean Hexagonal domain port (`AiProvider`):

1. **Universal Protocol Standard**: The outbound adapter issues standardized `POST /chat/completions` requests compatible with any OpenAI-compliant provider, local engine, or enterprise AI Gateway.
2. **Unified Configuration**: All AI settings are consolidated into a single configuration block in `application.yml`:
   ```yaml
   app:
     ai:
       enabled: ${AI_ENABLED:true}
       base-url: ${AI_BASE_URL:https://api.groq.com/openai/v1}
       api-key: ${AI_API_KEY:}
       model: ${AI_MODEL:llama-3.3-70b-versatile}
       temperature: ${AI_TEMPERATURE:0.2}
       fallback-enabled: ${AI_FALLBACK_ENABLED:true}
   ```
3. **Hexagonal Domain Purity**: Application services (`AiAssistantService`) interact solely with `AiProvider` domain ports with zero awareness of underlying AI vendors, models, or network topologies.
4. **Resilient Heuristic Fallback**: If the remote AI endpoint times out, encounters HTTP 429/500 errors, or if API keys are unconfigured (e.g., CI/CD or offline unit testing), the adapter automatically engages an internal rule-based heuristic synthesizer to guarantee 100% uptime.

## Consequences
### Positive
- **Zero Vendor Lock-In**: Switching providers from Groq to Gemini to self-hosted Ollama or an internal enterprise AI Gateway requires only updating `AI_BASE_URL` and `AI_MODEL` environment variables with zero code changes.
- **Zero Fragmented Property Files**: Eliminates vendor-specific YAML sprawl (`application-ai-*.yml`).
- **High Availability**: Heuristic fallback guarantees seamless graceful degradation during third-party AI outages or rate limiting.
- **Gateway-Ready**: Drop-in compatible with enterprise AI proxies and caching gateways.

### Considerations
- Token length limits and temperature parameters should be tuned per target model family.
