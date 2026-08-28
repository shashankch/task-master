package com.taskmaster.shared.config;

import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Configuration for AWS SDK v2 S3Client and S3Presigner with MinIO and S3 compatibility.
 */
@Configuration
public class S3ClientConfig {

    private static final Logger log = LoggerFactory.getLogger(S3ClientConfig.class);

    @Value("${app.storage.s3.endpoint:http://localhost:9000}")
    private String endpoint;

    @Value("${app.storage.s3.region:us-east-1}")
    private String region;

    @Value("${app.storage.s3.access-key:minioadmin}")
    private String accessKey;

    @Value("${app.storage.s3.secret-key:minioadmin}")
    private String secretKey;

    @Value("${app.storage.s3.path-style-access-enabled:true}")
    private boolean pathStyleAccessEnabled;

    @Bean
    public S3Client s3Client() {
        S3ClientBuilder builder = S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)
            ))
            .serviceConfiguration(S3Configuration.builder()
                .pathStyleAccessEnabled(pathStyleAccessEnabled)
                .build()
            );

        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        log.info("Initialized AWS SDK v2 S3Client with endpoint: {}, region: {}, pathStyle: {}",
            endpoint, region, pathStyleAccessEnabled);
        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        S3Presigner.Builder builder = S3Presigner.builder()
            .region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)
            ))
            .serviceConfiguration(S3Configuration.builder()
                .pathStyleAccessEnabled(pathStyleAccessEnabled)
                .build()
            );

        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        log.info("Initialized AWS SDK v2 S3Presigner with endpoint: {}, region: {}", endpoint, region);
        return builder.build();
    }
}
