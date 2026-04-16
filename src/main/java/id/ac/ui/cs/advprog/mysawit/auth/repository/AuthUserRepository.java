package id.ac.ui.cs.advprog.mysawit.auth.repository;

import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser;
import id.ac.ui.cs.advprog.mysawit.auth.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

        @Query("""
            SELECT u FROM AuthUser u
            WHERE (:name IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :name, '%')))
              AND (:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%')))
              AND (:role IS NULL OR u.role = :role)
            """)
        List<AuthUser> findUsersByFilters(
            @Param("name") String name,
            @Param("email") String email,
            @Param("role") Role role
        );
}