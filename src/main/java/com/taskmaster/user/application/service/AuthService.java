package com.taskmaster.user.application.service;

import com.taskmaster.shared.exception.ConflictException;
import com.taskmaster.shared.exception.ResourceNotFoundException;
import com.taskmaster.user.application.dto.AuthResponse;
import com.taskmaster.user.application.dto.LoginRequest;
import com.taskmaster.user.application.dto.RefreshTokenRequest;
import com.taskmaster.user.application.dto.RegisterRequest;
import com.taskmaster.user.application.dto.UserResponse;
import com.taskmaster.user.application.mapper.UserMapper;
import com.taskmaster.user.domain.model.User;
import com.taskmaster.user.domain.port.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service managing authentication use cases.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
        UserRepository userRepository,
        TokenService tokenService,
        UserMapper userMapper,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        String normalizedUsername = request.username().trim();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("Email '" + normalizedEmail + "' is already registered");
        }

        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new ConflictException("Username '" + normalizedUsername + "' is already taken");
        }

        String passwordHash = passwordEncoder.encode(request.password());
        User user = userMapper.toEntity(request, passwordHash);
        User savedUser = userRepository.save(user);

        return userMapper.toResponse(savedUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String identifier = request.identifier().trim();
        User user = userRepository.findByEmailOrUsername(identifier.toLowerCase(), identifier)
            .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        if (!user.isActive()) {
            throw new BadCredentialsException("Account has been deactivated");
        }

        String accessToken = tokenService.generateAccessToken(user);
        String refreshToken = tokenService.createRefreshToken(user.getId());
        long expiresInSeconds = tokenService.getAccessTokenExpirationSeconds();

        return AuthResponse.of(accessToken, refreshToken, expiresInSeconds, userMapper.toResponse(user));
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        TokenService.TokenRotationResult result = tokenService.rotateRefreshToken(request.refreshToken());
        User user = userRepository.findById(result.userId())
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", result.userId()));

        if (!user.isActive()) {
            throw new BadCredentialsException("Account has been deactivated");
        }

        String newAccessToken = tokenService.generateAccessToken(user);
        long expiresInSeconds = tokenService.getAccessTokenExpirationSeconds();

        return AuthResponse.of(newAccessToken, result.newRefreshToken(), expiresInSeconds, userMapper.toResponse(user));
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
            tokenService.revokeRefreshToken(request.refreshToken());
        }
    }
}
