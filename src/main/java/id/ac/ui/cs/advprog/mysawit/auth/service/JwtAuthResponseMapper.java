package id.ac.ui.cs.advprog.mysawit.auth.service;

import id.ac.ui.cs.advprog.mysawit.auth.dto.AuthResponse;
import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser;
import id.ac.ui.cs.advprog.mysawit.auth.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtAuthResponseMapper implements AuthResponseMapper {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public AuthResponse toAuthResponse(AuthUser user, String message) {
        String token = jwtTokenProvider.generateToken(
                user.getId().toString(),
                user.getEmail(),
                user.getRole().name(),
                user.getUsername(),
                user.getNama()
        );
        return AuthResponse.builder()
                .token(token)
                .id(user.getId().toString())
                .email(user.getEmail())
                .username(user.getUsername())
                .nama(user.getNama())
                .role(user.getRole().name())
                .message(message)
                .build();
    }
}
