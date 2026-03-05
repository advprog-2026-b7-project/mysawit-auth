package id.ac.ui.cs.advprog.mysawit.auth.controller;

import id.ac.ui.cs.advprog.mysawit.auth.dto.*;
import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser;
import id.ac.ui.cs.advprog.mysawit.auth.service.AuthUserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthUserController {

    private final AuthUserService authService;

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

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Logout successful. Please discard the token client-side."
                )
        );
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Not authenticated"));
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof AuthUser user)) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid authentication principal"));
        }

        MeResponse response = MeResponse.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .username(user.getUsername())
                .authProvider(user.getAuthProvider().toString())
                .build();

        return ResponseEntity.ok(response);
    }
}