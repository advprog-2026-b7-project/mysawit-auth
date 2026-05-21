package id.ac.ui.cs.advprog.mysawit.auth.repository;

import id.ac.ui.cs.advprog.mysawit.auth.entity.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, String> {
    void deleteByExpiresAtBefore(Instant threshold);
}
