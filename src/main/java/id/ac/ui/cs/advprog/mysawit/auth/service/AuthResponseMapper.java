package id.ac.ui.cs.advprog.mysawit.auth.service;

import id.ac.ui.cs.advprog.mysawit.auth.dto.AuthResponse;
import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser;

public interface AuthResponseMapper {
    AuthResponse toAuthResponse(AuthUser user, String message);
}
