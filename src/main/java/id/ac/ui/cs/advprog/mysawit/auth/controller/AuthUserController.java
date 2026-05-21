package id.ac.ui.cs.advprog.mysawit.auth.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.mysawit.auth.dto.*;
import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser;
import id.ac.ui.cs.advprog.mysawit.auth.security.JwtTokenProvider;
import id.ac.ui.cs.advprog.mysawit.auth.service.AuthUserService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;

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

    @Value("${cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${jwt.expiration:3600000}")
    private long jwtExpirationMs;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {
        AuthResponse authResponse = authService.register(request);
        response.addHeader(HttpHeaders.SET_COOKIE,
                buildAuthCookie(authResponse.getToken()).toString());
        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        AuthResponse authResponse = authService.login(request);
        response.addHeader(HttpHeaders.SET_COOKIE,
                buildAuthCookie(authResponse.getToken()).toString());
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/google-login")
    public ResponseEntity<AuthResponse> googleLogin(
            @Valid @RequestBody GoogleLoginRequest request,
            HttpServletResponse response) {
        AuthResponse authResponse = authService.googleLogin(request);
        response.addHeader(HttpHeaders.SET_COOKIE,
                buildAuthCookie(authResponse.getToken()).toString());
        return ResponseEntity.ok(authResponse);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestBody(required = false) LogoutRequest request,
            HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildClearCookie().toString());
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
            MeResponse meResponse = buildMeResponse(user);
            return ResponseEntity.ok(meResponse);
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
            MeResponse meResponse = buildMeResponse(user);
            return ResponseEntity.ok(meResponse);
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "User not found"));
        }
    }

    private ResponseCookie buildAuthCookie(String token) {
        return ResponseCookie.from("access_token", token)
                .httpOnly(true)
                .path("/")
                .maxAge(jwtExpirationMs / 1000)
                .sameSite("Lax")
                .secure(cookieSecure)
                .build();
    }

    private ResponseCookie buildClearCookie() {
        return ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .secure(cookieSecure)
                .build();
    }

    private MeResponse buildMeResponse(AuthUser user) {
        String mandorId = user.getMandor() != null
                ? user.getMandor().getId().toString() : null;
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
