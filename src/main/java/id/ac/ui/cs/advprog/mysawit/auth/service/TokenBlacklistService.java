package id.ac.ui.cs.advprog.mysawit.auth.service;

import java.time.Instant;

public interface TokenBlacklistService {
    void revoke(String jti, Instant expiresAt);
    boolean isRevoked(String jti);
}
