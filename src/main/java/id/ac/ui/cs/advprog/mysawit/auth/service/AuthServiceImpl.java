package id.ac.ui.cs.advprog.mysawit.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import id.ac.ui.cs.advprog.mysawit.auth.dto.AuthResponse;
import id.ac.ui.cs.advprog.mysawit.auth.dto.GoogleLoginRequest;
import id.ac.ui.cs.advprog.mysawit.auth.dto.LoginRequest;
import id.ac.ui.cs.advprog.mysawit.auth.dto.RegisterRequest;
import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthProvider;
import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser;
import id.ac.ui.cs.advprog.mysawit.auth.entity.Role;
import id.ac.ui.cs.advprog.mysawit.auth.repository.AuthUserRepository;
import id.ac.ui.cs.advprog.mysawit.auth.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthUserService {

    private final AuthUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${google.client-id}")
    private String googleClientId;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (request.getRole() == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Admin accounts cannot be registered directly");
        }

        if (request.getRole() == Role.MANDOR
                && (request.getMandorCertificationNumber() == null
                    || request.getMandorCertificationNumber().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Mandor accounts must provide a certification number");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already taken");
        }

        if (request.getRole() == Role.MANDOR
                && userRepository.existsByMandorCertificationNumber(
                        request.getMandorCertificationNumber())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Mandor certification number is already in use");
        }

        AuthUser user = AuthUser.builder()
                .username(request.getUsername())
                .nama(request.getNama())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .authProvider(AuthProvider.LOCAL)
                .mandorCertificationNumber(
                        request.getRole() == Role.MANDOR
                                ? request.getMandorCertificationNumber().trim()
                                : null
                )
                .build();

        user = userRepository.save(user);
        return buildAuthResponse(user, "User registered successfully.");
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        AuthUser user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (user.getPassword() == null
                || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        return buildAuthResponse(user, "Login successful.");
    }

    @Override
    public AuthResponse googleLogin(GoogleLoginRequest request) {
        GoogleIdToken.Payload payload = verifyGoogleToken(request.getIdToken());

        String email      = payload.getEmail();
        String googleName = (String) payload.get("name");

        Optional<AuthUser> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            return buildAuthResponse(existingUser.get(), "Google login successful.");
        }

        if (request.getRole() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Role is required for new user registration via Google");
        }

        Role role = request.getRole();

        if (role == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Admin accounts cannot be created via Google OAuth");
        }

        if (role == Role.MANDOR) {
            if (request.getMandorCertificationNumber() == null
                    || request.getMandorCertificationNumber().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Mandor certification number is required");
            }
            if (userRepository.existsByMandorCertificationNumber(
                    request.getMandorCertificationNumber())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Mandor certification number is already in use");
            }
        }

        String derivedUsername = deriveUsername(email);

        AuthUser newUser = AuthUser.builder()
                .username(derivedUsername)
                .nama(googleName != null ? googleName : derivedUsername)
                .email(email)
                .password(null)
                .role(role)
                .authProvider(AuthProvider.GOOGLE)
                .mandorCertificationNumber(
                        role == Role.MANDOR
                                ? request.getMandorCertificationNumber().trim()
                                : null
                )
                .build();

        newUser = userRepository.save(newUser);
        return buildAuthResponse(newUser, "User registered via Google.");
    }

    @Override
    public AuthUser getUserById(String userId) {
        return userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));
    }

    private GoogleIdToken.Payload verifyGoogleToken(String idToken) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();
            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Invalid Google ID token");
            }
            return token.getPayload();
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Google authentication failed", e);
        }
    }

    private String deriveUsername(String email) {
        String base = email.split("@")[0].replaceAll("[^a-zA-Z0-9_.]", "_");
        if (base.isBlank()) {
            return "user_" + UUID.randomUUID().toString().substring(0, 8);
        }
        if (!userRepository.existsByUsername(base)) {
            return base;
        }
        return base + "_" + UUID.randomUUID().toString().substring(0, 6);
    }

    private AuthResponse buildAuthResponse(AuthUser user, String message) {
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
