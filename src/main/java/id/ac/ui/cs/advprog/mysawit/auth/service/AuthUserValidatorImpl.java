package id.ac.ui.cs.advprog.mysawit.auth.service;

import id.ac.ui.cs.advprog.mysawit.auth.dto.GoogleLoginRequest;
import id.ac.ui.cs.advprog.mysawit.auth.dto.RegisterRequest;
import id.ac.ui.cs.advprog.mysawit.auth.entity.Role;
import id.ac.ui.cs.advprog.mysawit.auth.repository.AuthUserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class AuthUserValidatorImpl implements AuthUserValidator {

    private final AuthUserRepository userRepository;

    @Override
    public void validateRegistration(RegisterRequest request) {
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
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Email is already registered");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Username is already taken");
        }

        if (request.getRole() == Role.MANDOR
                && userRepository.existsByMandorCertificationNumber(
                        request.getMandorCertificationNumber())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Mandor certification number is already in use");
        }
    }

    @Override
    public void validateGoogleRegistration(GoogleLoginRequest request) {
        if (request.getRole() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Role is required for new user registration via Google");
        }

        if (request.getRole() == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Admin accounts cannot be created via Google OAuth");
        }

        if (request.getRole() == Role.MANDOR) {
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
    }
}
