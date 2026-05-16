package id.ac.ui.cs.advprog.mysawit.auth.repository;
import id.ac.ui.cs.advprog.mysawit.auth.entity.Assignment;
import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {
    List<Assignment> findByBuruh(AuthUser buruh);
    Optional<Assignment> findFirstByBuruh(AuthUser buruh);
    boolean existsByBuruh(AuthUser buruh);
    Page<Assignment> findByMandor(AuthUser mandor, Pageable pageable);
    Page<Assignment> findByBuruh(AuthUser buruh, Pageable pageable);
    List<Assignment> findByMandor(AuthUser mandor);
}
