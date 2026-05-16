package id.ac.ui.cs.advprog.mysawit.auth.repository;
import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser;
import id.ac.ui.cs.advprog.mysawit.auth.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface AuthUserRepository extends JpaRepository<AuthUser, UUID> {

    Optional<AuthUser> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByMandorCertificationNumber(String mandorCertificationNumber);

    /**
     * Paginated, case-insensitive filter on nama (or username fallback), email, and role.
     * All parameters are optional — pass null to skip.
     */
    @Query("""
            SELECT u FROM AuthUser u
            WHERE (:name IS NULL
                   OR LOWER(u.nama)     LIKE LOWER(CONCAT('%', :name, '%'))
                   OR LOWER(u.username) LIKE LOWER(CONCAT('%', :name, '%')))
              AND (:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%')))
              AND (:role  IS NULL OR u.role = :role)
            """)
    Page<AuthUser> findUsersByFilters(
            @Param("name")  String name,
            @Param("email") String email,
            @Param("role")  Role role,
            Pageable pageable
    );
}
