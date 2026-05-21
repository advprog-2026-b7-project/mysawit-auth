package id.ac.ui.cs.advprog.mysawit.auth.service;

import id.ac.ui.cs.advprog.mysawit.auth.entity.RevokedToken;
import id.ac.ui.cs.advprog.mysawit.auth.repository.RevokedTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private final RevokedTokenRepository revokedTokenRepository;

    @Override
    @Transactional
    public void revoke(String jti, Instant expiresAt) {
        if (!revokedTokenRepository.existsById(jti)) {
            revokedTokenRepository.save(
                    RevokedToken.builder().jti(jti).expiresAt(expiresAt).build()
            );
        }
    }

    @Override
    public boolean isRevoked(String jti) {
        return revokedTokenRepository.existsById(jti);
    }

    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void cleanupExpired() {
        revokedTokenRepository.deleteByExpiresAtBefore(Instant.now());
    }
}
