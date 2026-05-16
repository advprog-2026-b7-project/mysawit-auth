package id.ac.ui.cs.advprog.mysawit.auth.service;

import id.ac.ui.cs.advprog.mysawit.auth.dto.AuthResponse;
import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthProvider;
import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser;
import id.ac.ui.cs.advprog.mysawit.auth.entity.Role;
import id.ac.ui.cs.advprog.mysawit.auth.security.JwtTokenProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthResponseMapperTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private JwtAuthResponseMapper mapper;

    @Test
    void toAuthResponse_mapsAllFieldsCorrectly() {
        UUID id = UUID.randomUUID();
        AuthUser user = AuthUser.builder()
                .id(id)
                .username("alice")
                .nama("Alice")
                .email("alice@example.com")
                .role(Role.BURUH)
                .authProvider(AuthProvider.LOCAL)
                .build();
        String expectedToken = "jwt-token";

        when(jwtTokenProvider.generateToken(
                id.toString(), "alice@example.com", "BURUH", "alice", "Alice"))
                .thenReturn(expectedToken);

        AuthResponse result = mapper.toAuthResponse(user, "Login successful.");

        assertEquals(expectedToken, result.getToken());
        assertEquals(id.toString(), result.getId());
        assertEquals("alice@example.com", result.getEmail());
        assertEquals("alice", result.getUsername());
        assertEquals("Alice", result.getNama());
        assertEquals("BURUH", result.getRole());
        assertEquals("Login successful.", result.getMessage());

        verify(jwtTokenProvider).generateToken(
                id.toString(), "alice@example.com", "BURUH", "alice", "Alice");
    }
}
