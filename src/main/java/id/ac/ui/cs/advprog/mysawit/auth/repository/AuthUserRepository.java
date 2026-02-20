package id.ac.ui.cs.advprog.mysawit.auth.repository;

import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuthUserRepository extends JpaRepository<AuthUser, UUID> {
}