# ADR 0006: OpenTelemetry (OTel) Standard for Vendor-Neutral Observability

## Status
Accepted

## Context
Production-grade distributed systems require comprehensive observability across distributed tracing, application metrics, and structured log aggregation. Proprietary APM SDKs (e.g., Datadog, New Relic, Dynatrace, AWS X-Ray) create strong vendor lock-in, proprietary agent maintenance overhead, and steep licensing lock-in.

In modern cloud-native architectures (2026 standard):
- The **OpenTelemetry (OTel)** project governed by the Cloud Native Computing Foundation (CNCF) is the definitive industry standard for telemetry data generation and transmission.
- Standard **OpenTelemetry Protocol (OTLP)** over HTTP/gRPC enables applications to export telemetry to any compliant collector, gateway, or visualization backend with zero code modifications.
- Spring Boot provides first-class Micrometer Observation and OpenTelemetry tracing bridges (`micrometer-tracing-bridge-otel`, `opentelemetry-exporter-otlp`).

## Decision
We adopt **OpenTelemetry (OTel)** as the universal, vendor-neutral telemetry baseline for TaskMaster:

1. **Standard OTLP Export**: The application runtime exports distributed traces via standard OTLP HTTP/gRPC (`management.otlp.tracing.endpoint: ${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4318/v1/traces}`).
2. **Context Propagation (W3C TraceContext)**: Standard W3C `traceparent` and `tracestate` headers propagate across HTTP and WebSocket STOMP boundaries, coupled with MDC correlation IDs (`X-Correlation-ID`) for unified trace-to-log correlation.
3. **Pluggable Collector & Backend Architecture**:
   - **Local & Open-Source**: Zero-cost Jaeger all-in-one or OpenTelemetry Collector deployed via Docker Compose.
   - **Cloud Free-Tiers**: Drop-in compatible with Grafana Cloud, SigNoz, or Honeycomb free tiers.
   - **Enterprise Production**: Direct export to Datadog, Dynatrace, AWS X-Ray, or GCP Cloud Trace by changing only the environment variable endpoint.

## Consequences
### Positive
- **Zero Vendor Lock-In**: Telemetry backends can be swapped or multi-cast at the collector tier without recompiling or redeploying application code.
- **Unified Tracing & Logging**: MDC `correlationId` and OTel `traceId`/`spanId` provide end-to-end request visibility across REST controllers, domain event listeners, and WebSocket STOMP dispatchers.
- **Low Overhead**: Native Micrometer Tracing bridge introduces negligible CPU and memory footprint on virtual thread execution.

### Considerations
- Sampling probability is configurable via `management.tracing.sampling.probability` to balance observability detail with network/storage bandwidth in high-throughput environments.
