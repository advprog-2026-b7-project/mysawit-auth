package id.ac.ui.cs.advprog.mysawit.auth.repository;

import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser;
import id.ac.ui.cs.advprog.mysawit.auth.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface AuthUserRepository extends JpaRepository<AuthUser, UUID> {
    Optional<AuthUser> findByEmail(String email);
    boolean existsByEmail(String email);
    List<AuthUser> findByUsernameContainingIgnoreCase(String username);
    List<AuthUser> findByEmailContainingIgnoreCase(String email);
    List<AuthUser> findByRole(Role role);
    List<AuthUser> findByUsernameContainingIgnoreCaseAndEmailContainingIgnoreCase(
            String username,
            String email
    );
}