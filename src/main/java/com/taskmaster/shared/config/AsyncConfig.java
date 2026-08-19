package com.taskmaster.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {
    // Spring Boot virtual threads (`spring.threads.virtual.enabled=true`)
    // automatically powers TaskExecutor / Async tasks.
}
