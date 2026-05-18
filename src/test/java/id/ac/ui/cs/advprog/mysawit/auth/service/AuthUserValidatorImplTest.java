package id.ac.ui.cs.advprog.mysawit.auth.service;

import id.ac.ui.cs.advprog.mysawit.auth.dto.GoogleLoginRequest;
import id.ac.ui.cs.advprog.mysawit.auth.dto.RegisterRequest;
import id.ac.ui.cs.advprog.mysawit.auth.entity.Role;
import id.ac.ui.cs.advprog.mysawit.auth.repository.AuthUserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthUserValidatorImplTest {

    @Mock
    private AuthUserRepository userRepository;

    @InjectMocks
    private AuthUserValidatorImpl validator;


    @Test
    void validateRegistration_adminRole_throwsForbidden() {
        RegisterRequest req = RegisterRequest.builder()
                .username("admin").email("a@x.com").role(Role.ADMIN).build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> validator.validateRegistration(req));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertEquals("Admin accounts cannot be registered directly", ex.getReason());
    }

    @Test
    void validateRegistration_mandorNoCert_throwsBadRequest() {
        RegisterRequest req = RegisterRequest.builder()
                .username("m1").email("m1@x.com").role(Role.MANDOR)
                .mandorCertificationNumber("").build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> validator.validateRegistration(req));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Mandor accounts must provide a certification number", ex.getReason());
    }

    @Test
    void validateRegistration_duplicateEmail_throwsConflict() {
        RegisterRequest req = RegisterRequest.builder()
                .username("alice").email("alice@x.com").role(Role.BURUH).build();

        when(userRepository.existsByEmail("alice@x.com")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> validator.validateRegistration(req));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals("Email is already registered", ex.getReason());
    }

    @Test
    void validateRegistration_duplicateUsername_throwsConflict() {
        RegisterRequest req = RegisterRequest.builder()
                .username("alice").email("new@x.com").role(Role.BURUH).build();

        when(userRepository.existsByEmail("new@x.com")).thenReturn(false);
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> validator.validateRegistration(req));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals("Username is already taken", ex.getReason());
    }

    @Test
    void validateRegistration_duplicateCert_throwsConflict() {
        RegisterRequest req = RegisterRequest.builder()
                .username("m2").email("m2@x.com").role(Role.MANDOR)
                .mandorCertificationNumber("CERT-001").build();

        when(userRepository.existsByEmail("m2@x.com")).thenReturn(false);
        when(userRepository.existsByUsername("m2")).thenReturn(false);
        when(userRepository.existsByMandorCertificationNumber("CERT-001")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> validator.validateRegistration(req));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals("Mandor certification number is already in use", ex.getReason());
    }

    @Test
    void validateRegistration_success_noException() {
        RegisterRequest req = RegisterRequest.builder()
                .username("alice").email("alice@x.com").role(Role.BURUH).build();

        when(userRepository.existsByEmail("alice@x.com")).thenReturn(false);
        when(userRepository.existsByUsername("alice")).thenReturn(false);

        assertDoesNotThrow(() -> validator.validateRegistration(req));
    }


    @Test
    void validateGoogleRegistration_nullRole_throwsBadRequest() {
        GoogleLoginRequest req = GoogleLoginRequest.builder()
                .idToken("tok").role(null).build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> validator.validateGoogleRegistration(req));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Role is required for new user registration via Google", ex.getReason());
    }

    @Test
    void validateGoogleRegistration_adminRole_throwsForbidden() {
        GoogleLoginRequest req = GoogleLoginRequest.builder()
                .idToken("tok").role(Role.ADMIN).build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> validator.validateGoogleRegistration(req));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertEquals("Admin accounts cannot be created via Google OAuth", ex.getReason());
    }

    @Test
    void validateGoogleRegistration_mandorNoCert_throwsBadRequest() {
        GoogleLoginRequest req = GoogleLoginRequest.builder()
                .idToken("tok").role(Role.MANDOR).mandorCertificationNumber("").build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> validator.validateGoogleRegistration(req));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Mandor certification number is required", ex.getReason());
    }

    @Test
    void validateGoogleRegistration_mandorCertTaken_throwsConflict() {
        GoogleLoginRequest req = GoogleLoginRequest.builder()
                .idToken("tok").role(Role.MANDOR)
                .mandorCertificationNumber("CERT-007").build();

        when(userRepository.existsByMandorCertificationNumber("CERT-007")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> validator.validateGoogleRegistration(req));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals("Mandor certification number is already in use", ex.getReason());
    }

    @Test
    void validateGoogleRegistration_buruhSuccess_noException() {
        GoogleLoginRequest req = GoogleLoginRequest.builder()
                .idToken("tok").role(Role.BURUH).build();

        assertDoesNotThrow(() -> validator.validateGoogleRegistration(req));
    }
}
