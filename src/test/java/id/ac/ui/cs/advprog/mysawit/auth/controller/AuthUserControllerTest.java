package id.ac.ui.cs.advprog.mysawit.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.mysawit.auth.dto.AuthResponse;
import id.ac.ui.cs.advprog.mysawit.auth.dto.GoogleLoginRequest;
import id.ac.ui.cs.advprog.mysawit.auth.dto.LoginRequest;
import id.ac.ui.cs.advprog.mysawit.auth.dto.MeResponse;
import id.ac.ui.cs.advprog.mysawit.auth.dto.RegisterRequest;
import id.ac.ui.cs.advprog.mysawit.auth.dto.UpdateMeRequest;
import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthProvider;
import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser;
import id.ac.ui.cs.advprog.mysawit.auth.entity.Role;
import id.ac.ui.cs.advprog.mysawit.auth.repository.AuthUserRepository;
import id.ac.ui.cs.advprog.mysawit.auth.security.JwtTokenProvider;
import id.ac.ui.cs.advprog.mysawit.auth.service.AuthUserService;
import id.ac.ui.cs.advprog.mysawit.auth.service.TokenBlacklistService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:mysawit_auth;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private AuthUserRepository authUserRepository;

    @MockBean
    private AuthUserService authService;

    @Test
    void register_validBuruh_returns201() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .username("buruh01")
                .nama("Buruh Satu")
                .email("buruh01@test.com")
                .password("Pass1234!")
                .role(Role.BURUH)
                .build();

        AuthResponse resp = AuthResponse.builder()
                .token("tok")
                .id(UUID.randomUUID().toString())
                .email("buruh01@test.com")
                .role("BURUH")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("tok"));
    }

    @Test
    void register_setsAccessTokenCookie() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .username("buruh01")
                .nama("Buruh Satu")
                .email("buruh01@test.com")
                .password("Pass1234!")
                .role(Role.BURUH)
                .build();

        AuthResponse resp = AuthResponse.builder()
                .token("tok")
                .id(UUID.randomUUID().toString())
                .email("buruh01@test.com")
                .role("BURUH")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(result -> {
                    String setCookie = result.getResponse().getHeader("Set-Cookie");
                    assertThat(setCookie).contains("access_token=tok");
                    assertThat(setCookie).contains("HttpOnly");
                    assertThat(setCookie).contains("Path=/");
                });
    }

    @Test
    void register_weakPassword_returns400() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .username("buruh02")
                .nama("Buruh Dua")
                .email("buruh02@test.com")
                .password("weak")
                .role(Role.BURUH)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_missingFields_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_adminRole_returns403() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .username("adminX")
                .nama("Admin X")
                .email("adminx@test.com")
                .password("Pass1234!")
                .role(Role.ADMIN)
                .build();

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN,
                        "Cannot register as ADMIN"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .username("dup01")
                .nama("Dup One")
                .email("dup@test.com")
                .password("Pass1234!")
                .role(Role.BURUH)
                .build();

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new ResponseStatusException(CONFLICT, "Email already exists"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_mandorWithoutCert_returns400() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .username("mandorX")
                .nama("Mandor X")
                .email("mandorx@test.com")
                .password("Pass1234!")
                .role(Role.MANDOR)
                .build();

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "Certification number required for MANDOR"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_validCredentials_returns200() throws Exception {
        LoginRequest req = LoginRequest.builder()
                .email("admin@mysawit.com")
                .password("Admin1234!")
                .build();

        AuthResponse resp = AuthResponse.builder()
                .token("adminTok")
                .role("ADMIN")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("adminTok"));
    }

    @Test
    void login_setsAccessTokenCookie() throws Exception {
        LoginRequest req = LoginRequest.builder()
                .email("admin@mysawit.com")
                .password("Admin1234!")
                .build();

        AuthResponse resp = AuthResponse.builder()
                .token("adminTok")
                .role("ADMIN")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(result -> {
                    String setCookie = result.getResponse().getHeader("Set-Cookie");
                    assertThat(setCookie).contains("access_token=adminTok");
                    assertThat(setCookie).contains("HttpOnly");
                    assertThat(setCookie).contains("Path=/");
                });
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        LoginRequest req = LoginRequest.builder()
                .email("admin@mysawit.com")
                .password("wrongpass")
                .build();

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new ResponseStatusException(UNAUTHORIZED, "Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_unknownEmail_returns401_sameMessageAsWrongPassword() throws Exception {
        LoginRequest req = LoginRequest.builder()
                .email("nobody@nowhere.com")
                .password("Pass1234!")
                .build();

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new ResponseStatusException(UNAUTHORIZED, "Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void googleLogin_existingUser_returns200() throws Exception {
        GoogleLoginRequest req = GoogleLoginRequest.builder()
                .idToken("valid-google-token")
                .build();

        AuthResponse resp = AuthResponse.builder()
                .token("googleTok")
                .email("user@gmail.com")
                .build();

        when(authService.googleLogin(any(GoogleLoginRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/auth/google-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("googleTok"));
    }

    @Test
    void googleLogin_missingToken_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/google-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logout_withAuthentication_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        Authentication auth = buildAuthentication(userId, Role.BURUH);

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"some-token\"}")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout successful"));
    }

    @Test
    void logout_clearsCookie() throws Exception {
        UUID userId = UUID.randomUUID();
        Authentication auth = buildAuthentication(userId, Role.BURUH);

        mockMvc.perform(post("/api/auth/logout")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(result -> {
                    String setCookie = result.getResponse().getHeader("Set-Cookie");
                    assertThat(setCookie).contains("access_token=");
                    assertThat(setCookie).contains("Max-Age=0");
                });
    }

    @Test
    void logout_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"some-token\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_withBearerToken_revokesJti() throws Exception {
        AuthUser user = saveUser(UUID.randomUUID(), "logout-bearer@test.com", Role.BURUH);
        String token = jwtTokenProvider.generateToken(
                user.getId().toString(), "user@test.com", "BURUH", "user01", "User One");
        String jti = jwtTokenProvider.getJtiFromToken(token);

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout successful"));

        assertThat(tokenBlacklistService.isRevoked(jti)).isTrue();
    }

    @Test
    void logout_withCookie_revokesJti() throws Exception {
        AuthUser user = saveUser(UUID.randomUUID(), "logout-cookie@test.com", Role.BURUH);
        String token = jwtTokenProvider.generateToken(
                user.getId().toString(), "user@test.com", "BURUH", "user02", "User Two");
        String jti = jwtTokenProvider.getJtiFromToken(token);

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(new Cookie("access_token", token)))
                .andExpect(status().isOk());

        assertThat(tokenBlacklistService.isRevoked(jti)).isTrue();
    }

    @Test
    void logout_revokedToken_rejectsSubsequentRequest() throws Exception {
        AuthUser user = saveUser(UUID.randomUUID(), "logout-revoked@test.com", Role.BURUH);
        String token = jwtTokenProvider.generateToken(
                user.getId().toString(), "user@test.com", "BURUH", "user03", "User Three");

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void jwtForDeletedUser_rejectsSubsequentRequest() throws Exception {
        AuthUser user = saveUser(UUID.randomUUID(), "deleted-token@test.com", Role.BURUH);
        String token = jwtTokenProvider.generateToken(
                user.getId().toString(), user.getEmail(),
                "BURUH", user.getUsername(), user.getNama());

        authUserRepository.delete(user);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("User token has been revoked"));
    }

    @Test
    void getMe_withAuthentication_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        AuthUser user = buildAuthUser(userId, Role.BURUH);

        when(authService.getUserById(userId.toString())).thenReturn(user);

        mockMvc.perform(get("/api/auth/me")
                        .with(authentication(buildAuthentication(userId, Role.BURUH))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("buruh@test.com"))
                .andExpect(jsonPath("$.id").value(userId.toString()));
    }

    @Test
    void getMe_withCookieOnly_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        AuthUser user = buildAuthUser(userId, Role.BURUH);
        AuthUser savedUser = authUserRepository.save(user);

        String token = jwtTokenProvider.generateToken(
                savedUser.getId().toString(), "buruh@test.com", "BURUH", "buruh01", "Buruh Satu");

        when(authService.getUserById(savedUser.getId().toString())).thenReturn(savedUser);

        mockMvc.perform(get("/api/auth/me")
                        .cookie(new Cookie("access_token", token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("buruh@test.com"));
    }

    @Test
    void getMe_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMe_unknownUser_returns404() throws Exception {
        UUID userId = UUID.randomUUID();

        when(authService.getUserById(userId.toString()))
                .thenThrow(new RuntimeException("not found"));

        mockMvc.perform(get("/api/auth/me")
                        .with(authentication(buildAuthentication(userId, Role.BURUH))))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateMe_updateNama_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        UpdateMeRequest req = UpdateMeRequest.builder().nama("New Name").build();

        MeResponse resp = MeResponse.builder()
                .id(userId.toString())
                .email("buruh@test.com")
                .username("buruh01")
                .nama("New Name")
                .role("BURUH")
                .build();

        when(authService.updateMe(eq(userId.toString()), any(UpdateMeRequest.class)))
                .thenReturn(resp);

        mockMvc.perform(patch("/api/auth/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(authentication(buildAuthentication(userId, Role.BURUH))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nama").value("New Name"));
    }

    @Test
    void updateMe_emailFieldPresent_returns400() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(patch("/api/auth/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new@test.com\",\"nama\":\"Name\"}")
                        .with(authentication(buildAuthentication(userId, Role.BURUH))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateMe_duplicateUsername_returns409() throws Exception {
        UUID userId = UUID.randomUUID();
        UpdateMeRequest req = UpdateMeRequest.builder().username("taken").build();

        when(authService.updateMe(eq(userId.toString()), any(UpdateMeRequest.class)))
                .thenThrow(new ResponseStatusException(CONFLICT, "USERNAME_ALREADY_EXISTS"));

        mockMvc.perform(patch("/api/auth/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(authentication(buildAuthentication(userId, Role.BURUH))))
                .andExpect(status().isConflict());
    }

    @Test
    void updateMe_wrongCurrentPassword_returns400() throws Exception {
        UUID userId = UUID.randomUUID();
        UpdateMeRequest req = UpdateMeRequest.builder()
                .currentPassword("wrong")
                .newPassword("NewPass1!")
                .build();

        when(authService.updateMe(eq(userId.toString()), any(UpdateMeRequest.class)))
                .thenThrow(new ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "INVALID_CURRENT_PASSWORD"));

        mockMvc.perform(patch("/api/auth/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(authentication(buildAuthentication(userId, Role.BURUH))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateMe_oauthUserCannotChangePassword_returns400() throws Exception {
        UUID userId = UUID.randomUUID();
        UpdateMeRequest req = UpdateMeRequest.builder()
                .currentPassword("any")
                .newPassword("NewPass1!")
                .build();

        when(authService.updateMe(eq(userId.toString()), any(UpdateMeRequest.class)))
                .thenThrow(new ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "OAUTH_USER_CANNOT_CHANGE_PASSWORD"));

        mockMvc.perform(patch("/api/auth/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(authentication(buildAuthentication(userId, Role.BURUH))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateMe_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(patch("/api/auth/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nama\":\"Name\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getProfile_existingUser_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        AuthUser user = buildAuthUser(userId, Role.BURUH);

        when(authService.getUserById(userId.toString())).thenReturn(user);

        mockMvc.perform(get("/api/auth/profile/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("buruh@test.com"));
    }

    @Test
    void getProfile_unknownUser_returns404() throws Exception {
        UUID userId = UUID.randomUUID();

        when(authService.getUserById(userId.toString()))
                .thenThrow(new RuntimeException("not found"));

        mockMvc.perform(get("/api/auth/profile/" + userId))
                .andExpect(status().isNotFound());
    }

    private AuthUser buildAuthUser(UUID id, Role role) {
        return AuthUser.builder()
                .id(id)
                .username("buruh01")
                .nama("Buruh Satu")
                .email("buruh@test.com")
                .role(role)
                .authProvider(AuthProvider.LOCAL)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private AuthUser saveUser(UUID id, String email, Role role) {
        AuthUser user = AuthUser.builder()
                .id(id)
                .username(email.substring(0, email.indexOf('@')))
                .nama("Token User")
                .email(email)
                .password("encoded")
                .role(role)
                .authProvider(AuthProvider.LOCAL)
                .build();
        return authUserRepository.save(user);
    }

    private Authentication buildAuthentication(UUID userId, Role role) {
        AuthUser user = buildAuthUser(userId, role);
        return new UsernamePasswordAuthenticationToken(
                user,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );
    }
}
