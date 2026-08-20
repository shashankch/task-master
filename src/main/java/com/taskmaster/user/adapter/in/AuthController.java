package com.taskmaster.user.adapter.in;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.taskmaster.shared.dto.ApiResponse;
import com.taskmaster.user.application.dto.AuthResponse;
import com.taskmaster.user.application.dto.LoginRequest;
import com.taskmaster.user.application.dto.RefreshTokenRequest;
import com.taskmaster.user.application.dto.RegisterRequest;
import com.taskmaster.user.application.dto.UserResponse;
import com.taskmaster.user.application.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User registration, authentication, token rotation, and logout endpoints")
public class AuthController {

    private final AuthService authService;
    private final RSAKey rsaKey;

    public AuthController(AuthService authService, RSAKey rsaKey) {
        this.authService = authService;
        this.rsaKey = rsaKey;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user account")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and issue JWT access and refresh tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate refresh token and issue a new JWT access token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke user refresh token session")
    public ResponseEntity<ApiResponse<Map<String, String>>> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.of(Map.of("message", "Successfully logged out")));
    }

    @GetMapping("/.well-known/jwks.json")
    @Operation(summary = "JSON Web Key Set (JWKS) public verification keys")
    public Map<String, Object> getJwks() {
        return new JWKSet(rsaKey.toPublicJWK()).toJSONObject();
    }
}
