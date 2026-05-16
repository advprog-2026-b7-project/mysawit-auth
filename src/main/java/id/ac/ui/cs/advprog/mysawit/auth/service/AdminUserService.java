package id.ac.ui.cs.advprog.mysawit.auth.service;
import id.ac.ui.cs.advprog.mysawit.auth.dto.AdminUserResponse;
import id.ac.ui.cs.advprog.mysawit.auth.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface AdminUserService {
    Page<AdminUserResponse> getUsers(String name, String email, Role role, Pageable pageable);
    String deleteUser(UUID userId, UUID requesterId);
}
