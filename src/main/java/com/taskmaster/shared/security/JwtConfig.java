package com.taskmaster.shared.security;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Security configuration for RSA asymmetric key-pair loading and JWT encoding/decoding.
 * Supports loading persistent PEM keys for zero-session-loss restarts and multi-replica consistency.
 */
@Configuration
public class JwtConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);
    private static final String DEFAULT_KEY_ID = "taskmaster-primary-key";

    private final RSAKey rsaKey;

    public JwtConfig(
        @Value("${app.jwt.private-key-path:classpath:certs/private.pem}") String privateKeyPath,
        @Value("${app.jwt.public-key-path:classpath:certs/public.pem}") String publicKeyPath,
        ResourceLoader resourceLoader
    ) {
        this.rsaKey = loadOrCreateRsaKey(privateKeyPath, publicKeyPath, resourceLoader);
    }

    @Bean
    public RSAKey rsaKey() {
        return this.rsaKey;
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        JWKSet jwkSet = new JWKSet((JWK) this.rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        try {
            RSAPublicKey publicKey = this.rsaKey.toRSAPublicKey();
            return NimbusJwtDecoder.withPublicKey(publicKey).build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to configure RSA JwtDecoder", e);
        }
    }

    private static RSAKey loadOrCreateRsaKey(
        String privateKeyPath,
        String publicKeyPath,
        ResourceLoader resourceLoader
    ) {
        try {
            Resource privateResource = resourceLoader.getResource(privateKeyPath);
            Resource publicResource = resourceLoader.getResource(publicKeyPath);

            if (privateResource.exists() && publicResource.exists()) {
                String privateKeyPem = readResourceContent(privateResource);
                String publicKeyPem = readResourceContent(publicResource);

                RSAPrivateKey privateKey = parsePrivateKey(privateKeyPem);
                RSAPublicKey publicKey = parsePublicKey(publicKeyPem);

                log.info("Successfully loaded persistent RSA key pair from configured PEM paths: {}, {}",
                    privateKeyPath, publicKeyPath);

                return new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(DEFAULT_KEY_ID)
                    .build();
            } else {
                log.warn("Configured RSA PEM files not found at '{}' and '{}'. Falling back to in-memory key pair.",
                    privateKeyPath, publicKeyPath);
                return generateEphemeralRsaKey();
            }
        } catch (Exception e) {
            log.warn("Could not load RSA keys from PEM files ({}). Falling back to ephemeral key pair.",
                e.getMessage());
            return generateEphemeralRsaKey();
        }
    }

    private static String readResourceContent(Resource resource) throws Exception {
        try (InputStream is = resource.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static RSAPrivateKey parsePrivateKey(String pem) throws Exception {
        String cleanPem = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("-----BEGIN RSA PRIVATE KEY-----", "")
            .replace("-----END RSA PRIVATE KEY-----", "")
            .replaceAll("\\s+", "");

        byte[] decoded = Base64.getDecoder().decode(cleanPem);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) factory.generatePrivate(spec);
    }

    private static RSAPublicKey parsePublicKey(String pem) throws Exception {
        String cleanPem = pem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("-----BEGIN RSA PUBLIC KEY-----", "")
            .replace("-----END RSA PUBLIC KEY-----", "")
            .replaceAll("\\s+", "");

        byte[] decoded = Base64.getDecoder().decode(cleanPem);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) factory.generatePublic(spec);
    }

    private static RSAKey generateEphemeralRsaKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
            return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(DEFAULT_KEY_ID)
                .build();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA algorithm not supported by JVM", e);
        }
    }
}
