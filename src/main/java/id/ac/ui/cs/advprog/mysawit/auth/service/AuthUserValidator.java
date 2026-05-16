package id.ac.ui.cs.advprog.mysawit.auth.service;

import id.ac.ui.cs.advprog.mysawit.auth.dto.GoogleLoginRequest;
import id.ac.ui.cs.advprog.mysawit.auth.dto.RegisterRequest;

public interface AuthUserValidator {
    void validateRegistration(RegisterRequest request);
    void validateGoogleRegistration(GoogleLoginRequest request);
}
