package id.ac.ui.cs.advprog.mysawit.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;

import id.ac.ui.cs.advprog.mysawit.auth.dto.AuthResponse;
import id.ac.ui.cs.advprog.mysawit.auth.dto.GoogleLoginRequest;
import id.ac.ui.cs.advprog.mysawit.auth.dto.LoginRequest;
import id.ac.ui.cs.advprog.mysawit.auth.dto.RegisterRequest;
import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthProvider;
import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser;
import id.ac.ui.cs.advprog.mysawit.auth.entity.Role;
import id.ac.ui.cs.advprog.mysawit.auth.repository.AuthUserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthUserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private GoogleTokenVerifier googleTokenVerifier;

    @Mock
    private UsernameGenerator usernameGenerator;

    @Mock
    private AuthResponseMapper authResponseMapper;

    @Mock
    private AuthUserValidator authUserValidator;

    @InjectMocks
    private AuthServiceImpl authService;

    private AuthUser buildUser(UUID id, Role role) {
        return AuthUser.builder()
                .id(id)
                .username("alice")
                .nama("Alice")
                .email("alice@example.com")
                .password("hashed")
                .role(role)
                .authProvider(AuthProvider.LOCAL)
                .build();
    }

    private AuthResponse dummyResponse(String message) {
        return AuthResponse.builder()
                .token("tok")
                .id(UUID.randomUUID().toString())
                .email("alice@example.com")
                .username("alice")
                .nama("Alice")
                .role(Role.BURUH.name())
                .message(message)
                .build();
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    void register_success() {
        RegisterRequest req = RegisterRequest.builder()
                .username("alice").nama("Alice").email("alice@example.com")
                .password("Pass1!abc").role(Role.BURUH).build();

        AuthUser saved = buildUser(UUID.randomUUID(), Role.BURUH);
        AuthResponse expected = dummyResponse("User registered successfully.");

        when(passwordEncoder.encode(req.getPassword())).thenReturn("hashed");
        when(userRepository.save(any(AuthUser.class))).thenReturn(saved);
        when(authResponseMapper.toAuthResponse(saved, "User registered successfully."))
                .thenReturn(expected);

        AuthResponse result = authService.register(req);

        assertEquals(expected, result);
        verify(authUserValidator).validateRegistration(req);
        verify(userRepository).save(any(AuthUser.class));
    }

    @Test
    void register_adminRoleForbidden() {
        RegisterRequest req = RegisterRequest.builder()
                .username("admin").nama("Admin").email("admin@example.com")
                .password("Pass1!abc").role(Role.ADMIN).build();

        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Admin accounts cannot be registered directly"))
                .when(authUserValidator).validateRegistration(req);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.register(req));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_mandorNoCert() {
        RegisterRequest req = RegisterRequest.builder()
                .username("mandor1").nama("Mandor").email("m@example.com")
                .password("Pass1!abc").role(Role.MANDOR).build();

        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Mandor accounts must provide a certification number"))
                .when(authUserValidator).validateRegistration(req);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.register(req));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void register_emailConflict() {
        RegisterRequest req = RegisterRequest.builder()
                .username("alice").nama("Alice").email("alice@example.com")
                .password("Pass1!abc").role(Role.BURUH).build();

        doThrow(new ResponseStatusException(HttpStatus.CONFLICT,
                "Email is already registered"))
                .when(authUserValidator).validateRegistration(req);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.register(req));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void register_usernameConflict() {
        RegisterRequest req = RegisterRequest.builder()
                .username("alice").nama("Alice").email("new@example.com")
                .password("Pass1!abc").role(Role.BURUH).build();

        doThrow(new ResponseStatusException(HttpStatus.CONFLICT,
                "Username is already taken"))
                .when(authUserValidator).validateRegistration(req);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.register(req));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void register_certConflict() {
        RegisterRequest req = RegisterRequest.builder()
                .username("mandor2").nama("Mandor2").email("m2@example.com")
                .password("Pass1!abc").role(Role.MANDOR)
                .mandorCertificationNumber("CERT-001").build();

        doThrow(new ResponseStatusException(HttpStatus.CONFLICT,
                "Mandor certification number is already in use"))
                .when(authUserValidator).validateRegistration(req);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.register(req));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    void login_success() {
        LoginRequest req = LoginRequest.builder()
                .email("alice@example.com").password("Pass1!abc").build();
        AuthUser user = buildUser(UUID.randomUUID(), Role.BURUH);
        AuthResponse expected = dummyResponse("Login successful.");

        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(req.getPassword(), user.getPassword())).thenReturn(true);
        when(authResponseMapper.toAuthResponse(user, "Login successful.")).thenReturn(expected);

        AuthResponse result = authService.login(req);

        assertEquals(expected, result);
    }

    @Test
    void login_userNotFound() {
        LoginRequest req = LoginRequest.builder()
                .email("nobody@example.com").password("Pass1!abc").build();

        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.login(req));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertEquals("Invalid email or password", ex.getReason());
    }

    @Test
    void login_wrongPassword() {
        LoginRequest req = LoginRequest.builder()
                .email("alice@example.com").password("wrong").build();
        AuthUser user = buildUser(UUID.randomUUID(), Role.BURUH);

        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(req.getPassword(), user.getPassword())).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.login(req));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void login_nullPassword_googleUser() {
        LoginRequest req = LoginRequest.builder()
                .email("alice@example.com").password("anypass").build();
        AuthUser user = AuthUser.builder()
                .id(UUID.randomUUID())
                .username("alice")
                .email("alice@example.com")
                .role(Role.BURUH)
                .password(null)
                .authProvider(AuthProvider.GOOGLE)
                .build();

        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(user));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.login(req));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    // ── googleLogin ───────────────────────────────────────────────────────────

    private GoogleIdToken.Payload buildPayload(String email, String name) {
        GoogleIdToken.Payload payload = mock(GoogleIdToken.Payload.class);
        when(payload.getEmail()).thenReturn(email);
        when(payload.get("name")).thenReturn(name);
        return payload;
    }

    @Test
    void googleLogin_existingUser() {
        GoogleLoginRequest req = GoogleLoginRequest.builder()
                .idToken("id-token").build();
        GoogleIdToken.Payload payload = buildPayload("alice@example.com", "Alice");
        AuthUser user = buildUser(UUID.randomUUID(), Role.BURUH);
        AuthResponse expected = dummyResponse("Google login successful.");

        when(googleTokenVerifier.verify(req.getIdToken())).thenReturn(payload);
        when(userRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(user));
        when(authResponseMapper.toAuthResponse(user, "Google login successful."))
                .thenReturn(expected);

        AuthResponse result = authService.googleLogin(req);

        assertEquals(expected, result);
        verify(authUserValidator, never()).validateGoogleRegistration(any());
    }

    @Test
    void googleLogin_newUserSuccess() {
        GoogleLoginRequest req = GoogleLoginRequest.builder()
                .idToken("id-token").role(Role.BURUH).build();
        GoogleIdToken.Payload payload = buildPayload("new@example.com", "New User");
        AuthUser saved = buildUser(UUID.randomUUID(), Role.BURUH);
        AuthResponse expected = dummyResponse("User registered via Google.");

        when(googleTokenVerifier.verify(req.getIdToken())).thenReturn(payload);
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(usernameGenerator.generate("new@example.com")).thenReturn("new");
        when(userRepository.save(any(AuthUser.class))).thenReturn(saved);
        when(authResponseMapper.toAuthResponse(saved, "User registered via Google."))
                .thenReturn(expected);

        AuthResponse result = authService.googleLogin(req);

        assertEquals(expected, result);
        verify(authUserValidator).validateGoogleRegistration(req);
        verify(usernameGenerator).generate("new@example.com");
    }

    @Test
    void googleLogin_newMandorUserSuccess() {
        GoogleLoginRequest req = GoogleLoginRequest.builder()
                .idToken("id-token").role(Role.MANDOR)
                .mandorCertificationNumber("CERT-999").build();
        GoogleIdToken.Payload payload = buildPayload("mandor@example.com", "Mandor User");
        AuthUser saved = buildUser(UUID.randomUUID(), Role.MANDOR);
        AuthResponse expected = dummyResponse("User registered via Google.");

        when(googleTokenVerifier.verify(req.getIdToken())).thenReturn(payload);
        when(userRepository.findByEmail("mandor@example.com")).thenReturn(Optional.empty());
        when(usernameGenerator.generate("mandor@example.com")).thenReturn("mandor");
        when(userRepository.save(any(AuthUser.class))).thenReturn(saved);
        when(authResponseMapper.toAuthResponse(saved, "User registered via Google."))
                .thenReturn(expected);

        AuthResponse result = authService.googleLogin(req);

        assertEquals(expected, result);
        verify(authUserValidator).validateGoogleRegistration(req);

        ArgumentCaptor<AuthUser> captor = ArgumentCaptor.forClass(AuthUser.class);
        verify(userRepository).save(captor.capture());
        assertEquals("CERT-999", captor.getValue().getMandorCertificationNumber());
    }

    @Test
    void googleLogin_validatorDelegated() {
        GoogleLoginRequest req = GoogleLoginRequest.builder()
                .idToken("id-token").build();
        GoogleIdToken.Payload payload = buildPayload("new@example.com", "New");

        when(googleTokenVerifier.verify(req.getIdToken())).thenReturn(payload);
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Role is required for new user registration via Google"))
                .when(authUserValidator).validateGoogleRegistration(req);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.googleLogin(req));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(authUserValidator).validateGoogleRegistration(req);
    }

    // ── getUserById ───────────────────────────────────────────────────────────

    @Test
    void getUserById_success() {
        UUID id = UUID.randomUUID();
        AuthUser user = buildUser(id, Role.BURUH);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        AuthUser result = authService.getUserById(id.toString());

        assertEquals(user, result);
    }

    @Test
    void getUserById_notFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.getUserById(id.toString()));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("User not found", ex.getReason());
    }
}
