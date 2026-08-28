package com.taskmaster.shared.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Cross-Origin Resource Sharing (CORS) configuration with explicit origin allowlist.
 */
@Configuration
public class CorsConfig {

    private final List<String> allowedOrigins;

    public CorsConfig(
        @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173,http://localhost:8080}")
        List<String> allowedOrigins
    ) {
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOriginPatterns(allowedOrigins);
        config.setAllowedHeaders(List.of(
            "Origin", "Content-Type", "Accept", "Authorization",
            "X-Requested-With", "Access-Control-Request-Method",
            "Access-Control-Request-Headers", "Idempotency-Key", "X-Correlation-ID"
        ));
        config.setExposedHeaders(List.of(
            "Access-Control-Allow-Origin", "Access-Control-Allow-Credentials",
            "ETag", "Location", "X-Correlation-ID"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
