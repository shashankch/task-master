package com.taskmaster.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.taskmaster.shared.exception.ConflictException;
import com.taskmaster.user.application.dto.AuthResponse;
import com.taskmaster.user.application.dto.LoginRequest;
import com.taskmaster.user.application.dto.RefreshTokenRequest;
import com.taskmaster.user.application.dto.RegisterRequest;
import com.taskmaster.user.application.dto.UserResponse;
import com.taskmaster.user.application.mapper.UserMapper;
import com.taskmaster.user.domain.model.Role;
import com.taskmaster.user.domain.model.User;
import com.taskmaster.user.domain.port.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenService tokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserMapper userMapper;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userMapper = new UserMapper();
        authService = new AuthService(userRepository, tokenService, userMapper, passwordEncoder);
    }

    @Test
    @DisplayName("Should register user successfully when email and username are unique")
    void register_WhenValidRequest_ShouldCreateUser() {
        RegisterRequest request = new RegisterRequest("test@example.com", "testuser", "Password@123", "Test User");
        User user = new User("test@example.com", "testuser", "hashed_pw", "Test User", Role.USER);
        user.setId(UUID.randomUUID());

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("hashed_pw");
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.username()).isEqualTo("testuser");
        assertThat(response.displayName()).isEqualTo("Test User");
        assertThat(response.role()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("Should throw ConflictException when email is already registered")
    void register_WhenDuplicateEmail_ShouldThrowConflict() {
        RegisterRequest request = new RegisterRequest("test@example.com", "testuser", "Password@123", "Test User");
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Email 'test@example.com' is already registered");
    }

    @Test
    @DisplayName("Should throw ConflictException when username is already taken")
    void register_WhenDuplicateUsername_ShouldThrowConflict() {
        RegisterRequest request = new RegisterRequest("test@example.com", "testuser", "Password@123", "Test User");
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Username 'testuser' is already taken");
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void login_WhenValidCredentials_ShouldReturnAuthResponse() {
        LoginRequest request = new LoginRequest("testuser", "Password@123");
        User user = new User("test@example.com", "testuser", "hashed_pw", "Test User", Role.USER);
        user.setId(UUID.randomUUID());

        when(userRepository.findByEmailOrUsername("testuser", "testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password@123", "hashed_pw")).thenReturn(true);
        when(tokenService.generateAccessToken(user)).thenReturn("access-token-123");
        when(tokenService.createRefreshToken(user.getId())).thenReturn("refresh-token-456");
        when(tokenService.getAccessTokenExpirationSeconds()).thenReturn(900L);

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("access-token-123");
        assertThat(response.refreshToken()).isEqualTo("refresh-token-456");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().email()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when password does not match")
    void login_WhenInvalidPassword_ShouldThrowBadCredentials() {
        LoginRequest request = new LoginRequest("testuser", "WrongPassword");
        User user = new User("test@example.com", "testuser", "hashed_pw", "Test User", Role.USER);
        user.setId(UUID.randomUUID());

        when(userRepository.findByEmailOrUsername("testuser", "testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword", "hashed_pw")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessageContaining("Invalid username or password");
    }

    @Test
    @DisplayName("Should refresh token successfully")
    void refreshToken_WhenValidToken_ShouldReturnNewAuthResponse() {
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");
        UUID userId = UUID.randomUUID();
        User user = new User("test@example.com", "testuser", "hashed_pw", "Test User", Role.USER);
        user.setId(userId);

        when(tokenService.rotateRefreshToken("valid-refresh-token"))
            .thenReturn(new TokenService.TokenRotationResult("new-refresh-token", userId));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tokenService.generateAccessToken(user)).thenReturn("new-access-token");
        when(tokenService.getAccessTokenExpirationSeconds()).thenReturn(900L);

        AuthResponse response = authService.refreshToken(request);

        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
    }

    @Test
    @DisplayName("Should invoke token revocation on logout")
    void logout_WhenCalled_ShouldRevokeRefreshToken() {
        RefreshTokenRequest request = new RefreshTokenRequest("token-to-revoke");
        authService.logout(request);
        verify(tokenService).revokeRefreshToken("token-to-revoke");
    }
}
