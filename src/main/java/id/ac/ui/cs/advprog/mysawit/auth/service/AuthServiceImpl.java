package id.ac.ui.cs.advprog.mysawit.auth.service;

import com.google.api.client.googleapis.auth.oauth2
        .GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2
        .GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import id.ac.ui.cs.advprog.mysawit.auth.dto.AuthResponse;
import id.ac.ui.cs.advprog.mysawit.auth.dto.GoogleLoginRequest;
import id.ac.ui.cs.advprog.mysawit.auth.dto.LoginRequest;
import id.ac.ui.cs.advprog.mysawit.auth.dto.RegisterRequest;
import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser;
import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser.AuthProvider;
import id.ac.ui.cs.advprog.mysawit.auth.repository.AuthUserRepository;
import id.ac.ui.cs.advprog.mysawit.auth.security
        .JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password
        .PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.Optional;
import id.ac.ui.cs.advprog.mysawit.auth.entity.Role;

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
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Email is already registered"
            );
        }

        AuthUser user = AuthUser.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .username(request.getName())
                .role(request.getRole())
                .authProvider(AuthProvider.LOCAL)
                .build();

        user = userRepository.save(user);
        String token = jwtTokenProvider.generateToken(
                user.getId().toString(),
                user.getEmail(),
                user.getRole().name()
        );

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .name(user.getUsername())
                .message("Registration successful")
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        AuthUser user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid email or password"
                ));

        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "This account uses Google login. "
                            + "Please sign in with Google."
            );
        }

        if (!passwordEncoder.matches(
                request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password"
            );
        }

        String token = jwtTokenProvider.generateToken(
                user.getId().toString(),
                user.getEmail(),
                user.getRole().name()
        );

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .name(user.getUsername())
                .message("Login successful")
                .build();
    }

    @Override
    public AuthResponse googleLogin(
            GoogleLoginRequest request) {
        try {
            GoogleIdTokenVerifier verifier =
                    new GoogleIdTokenVerifier.Builder(
                            new NetHttpTransport(),
                            GsonFactory.getDefaultInstance()
                    )
                            .setAudience(
                                    Collections.singletonList(googleClientId)
                            )
                            .build();

            GoogleIdToken idToken =
                    verifier.verify(request.getIdToken());
            if (idToken == null) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid Google ID token"
                );
            }

            GoogleIdToken.Payload payload =
                    idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");

            Optional<AuthUser> existingUser =
                    userRepository.findByEmail(email);
            AuthUser user;

            if (existingUser.isPresent()) {
                user = existingUser.get();
                if (user.getAuthProvider()
                        != AuthProvider.GOOGLE) {
                    throw new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED,
                            "This email is registered with "
                                    + "a local account. "
                                    + "Please login with password."
                    );
                }
            } else {
                user = AuthUser.builder()
                        .email(email)
                        .username(name != null ? name : email)
                        .role(Role.BURUH)   // default role
                        .authProvider(AuthProvider.GOOGLE)
                        .build();
                user = userRepository.save(user);
            }

            String token = jwtTokenProvider.generateToken(
                    user.getId().toString(),
                    user.getEmail(),
                    user.getRole().name()
            );
            return AuthResponse.builder()
                    .token(token)
                    .email(user.getEmail())
                    .name(user.getUsername())
                    .message("Google login successful")
                    .build();

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Google authentication failed",
                    e
            );
        }
    }
}