package id.ac.ui.cs.advprog.mysawit.auth.repository;
import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser;
import id.ac.ui.cs.advprog.mysawit.auth.entity.Role;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuthUserRepository
        extends JpaRepository<AuthUser, UUID>, JpaSpecificationExecutor<AuthUser> {

    Optional<AuthUser> findByEmail(String email);
    Optional<AuthUser> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByMandorCertificationNumber(String mandorCertificationNumber);

    default Page<AuthUser> findUsersByFilters(
            String name, String email, Role role, Pageable pageable) {
        return findAll((root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (name != null) {
                String pattern = "%" + name.toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("nama")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), pattern)
                ));
            }

            if (email != null) {
                String pattern = "%" + email.toLowerCase() + "%";
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("email")), pattern));
            }

            if (role != null) {
                predicates.add(criteriaBuilder.equal(root.get("role"), role));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        }, pageable);
    }
}
