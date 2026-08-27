package com.taskmaster.user.application.service;

import com.taskmaster.user.domain.model.RefreshToken;
import com.taskmaster.user.domain.model.User;
import com.taskmaster.user.domain.port.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for JWT access token encoding and refresh token lifecycle management.
 */
@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final String issuer;
    private final Duration accessTokenExpiry;
    private final Duration refreshTokenExpiry;
    private final SecureRandom secureRandom = new SecureRandom();

    public TokenService(
        JwtEncoder jwtEncoder,
        RefreshTokenRepository refreshTokenRepository,
        @Value("${app.jwt.issuer:taskmaster-auth-service}") String issuer,
        @Value("${app.jwt.access-token-expiration-minutes:15}") int accessTokenExpirationMinutes,
        @Value("${app.jwt.refresh-token-expiration-days:7}") int refreshTokenExpirationDays
    ) {
        this.jwtEncoder = jwtEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.issuer = issuer;
        this.accessTokenExpiry = Duration.ofMinutes(accessTokenExpirationMinutes);
        this.refreshTokenExpiry = Duration.ofDays(refreshTokenExpirationDays);
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(accessTokenExpiry);

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(issuer)
            .issuedAt(now)
            .expiresAt(expiresAt)
            .subject(user.getId().toString())
            .claim("email", user.getEmail())
            .claim("username", user.getUsername())
            .claim("roles", List.of(user.getRole().name()))
            .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpiry.toSeconds();
    }

    @Transactional
    public String createRefreshToken(UUID userId) {
        String rawToken = generateSecureRandomToken();
        String familyId = UUID.randomUUID().toString();
        String tokenHash = hashToken(rawToken);
        Instant expiresAt = Instant.now().plus(refreshTokenExpiry);

        RefreshToken refreshToken = new RefreshToken(tokenHash, userId, familyId, expiresAt);
        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    @Transactional
    public TokenRotationResult rotateRefreshToken(String rawToken) {
        String tokenHash = hashToken(rawToken);
        RefreshToken currentToken = refreshTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (currentToken.isRevoked()) {
            // Theft detection: Replay of a revoked token triggers revocation of the entire family
            refreshTokenRepository.revokeAllByFamilyId(currentToken.getFamilyId());
            throw new BadCredentialsException("Refresh token was previously revoked. Token family invalidated.");
        }

        if (currentToken.isExpired()) {
            throw new BadCredentialsException("Refresh token has expired");
        }

        // Invalidate current token
        currentToken.setRevoked(true);
        refreshTokenRepository.save(currentToken);

        // Issue new token in same family
        String newRawToken = generateSecureRandomToken();
        String newTokenHash = hashToken(newRawToken);
        Instant newExpiresAt = Instant.now().plus(refreshTokenExpiry);

        RefreshToken newRefreshToken = new RefreshToken(
            newTokenHash,
            currentToken.getUserId(),
            currentToken.getFamilyId(),
            newExpiresAt
        );
        refreshTokenRepository.save(newRefreshToken);

        return new TokenRotationResult(newRawToken, currentToken.getUserId());
    }

    @Transactional
    public void revokeRefreshToken(String rawToken) {
        String tokenHash = hashToken(rawToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    private String generateSecureRandomToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    public record TokenRotationResult(String newRefreshToken, UUID userId) {}
}
