package id.ac.ui.cs.advprog.mysawit.auth.service;

import id.ac.ui.cs.advprog.mysawit.auth.dto.AuthResponse;
import id.ac.ui.cs.advprog.mysawit.auth.dto.GoogleLoginRequest;
import id.ac.ui.cs.advprog.mysawit.auth.dto.LoginRequest;
import id.ac.ui.cs.advprog.mysawit.auth.dto.RegisterRequest;
import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthProvider;
import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser;
import id.ac.ui.cs.advprog.mysawit.auth.entity.Role;
import id.ac.ui.cs.advprog.mysawit.auth.repository.AuthUserRepository;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthUserService {

    private final AuthUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final UsernameGenerator usernameGenerator;
    private final AuthResponseMapper authResponseMapper;
    private final AuthUserValidator authUserValidator;

    @Override
    public AuthResponse register(RegisterRequest request) {
        authUserValidator.validateRegistration(request);

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
        return authResponseMapper.toAuthResponse(user, "User registered successfully.");
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        AuthUser user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (user.getPassword() == null
                || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        return authResponseMapper.toAuthResponse(user, "Login successful.");
    }

    @Override
    public AuthResponse googleLogin(GoogleLoginRequest request) {
        GoogleIdToken.Payload payload = googleTokenVerifier.verify(request.getIdToken());

        String email = payload.getEmail();
        String googleName = (String) payload.get("name");

        Optional<AuthUser> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            return authResponseMapper.toAuthResponse(
                    existingUser.get(), "Google login successful.");
        }

        authUserValidator.validateGoogleRegistration(request);

        Role role = request.getRole();
        String derivedUsername = usernameGenerator.generate(email);

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
        return authResponseMapper.toAuthResponse(newUser, "User registered via Google.");
    }

    @Override
    public AuthUser getUserById(String userId) {
        return userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));
    }
}
