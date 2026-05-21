package id.ac.ui.cs.advprog.mysawit.auth.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.mysawit.auth.dto.*;
import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser;
import id.ac.ui.cs.advprog.mysawit.auth.security.JwtTokenProvider;
import id.ac.ui.cs.advprog.mysawit.auth.service.AuthUserService;
import id.ac.ui.cs.advprog.mysawit.auth.service.TokenBlacklistService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthUserController {

    private final AuthUserService authService;
    private final ObjectMapper objectMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/google-login")
    public ResponseEntity<AuthResponse> googleLogin(
            @Valid @RequestBody GoogleLoginRequest request) {
        AuthResponse response = authService.googleLogin(request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @Valid @RequestBody LogoutRequest request) {
        try {
            String jti = jwtTokenProvider.getJtiFromToken(request.getToken());
            Instant expiresAt = jwtTokenProvider.getExpirationFromToken(request.getToken());
            tokenBlacklistService.revoke(jti, expiresAt);
        } catch (Exception e) {
            log.warn("Token revocation skipped: {}", e.getMessage());
        }
        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Not authenticated"));
        }

        if (!(authentication.getPrincipal() instanceof AuthUser tokenUser)) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid authentication principal"));
        }

        try {
            AuthUser user = authService.getUserById(tokenUser.getId().toString());
            MeResponse response = buildMeResponse(user);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "User not found"));
        }
    }

    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/me")
    public ResponseEntity<MeResponse> updateCurrentUser(
            Authentication authentication,
            @RequestBody JsonNode rawRequestBody) {

        if (rawRequestBody.has("email")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "EMAIL_NOT_EDITABLE");
        }

        AuthUser tokenUser = extractTokenUser(authentication);
        UpdateMeRequest request = objectMapper.convertValue(rawRequestBody, UpdateMeRequest.class);
        return ResponseEntity.ok(authService.updateMe(tokenUser.getId().toString(), request));
    }

    @GetMapping("/profile/{userId}")
    public ResponseEntity<?> getUserProfile(@PathVariable String userId) {
        try {
            AuthUser user = authService.getUserById(userId);
            MeResponse response = buildMeResponse(user);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "User not found"));
        }
    }

    private MeResponse buildMeResponse(AuthUser user) {
        String mandorId = user.getMandor() != null ? user.getMandor().getId().toString() : null;
        return MeResponse.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .username(user.getUsername())
                .nama(user.getNama())
                .role(user.getRole().toString())
                .walletBalance(user.getWalletBalance() != null
                        ? user.getWalletBalance()
                        : BigDecimal.ZERO)
                .mandorCertificationNumber(user.getMandorCertificationNumber())
                .mandorId(mandorId)
                .authProvider(user.getAuthProvider() != null
                        ? user.getAuthProvider().name() : "LOCAL")
                .createdAt(user.getCreatedAt())
                .build();
    }

    private AuthUser extractTokenUser(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AuthUser tokenUser)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "TOKEN_MISSING_OR_INVALID");
        }
        return tokenUser;
    }
}
