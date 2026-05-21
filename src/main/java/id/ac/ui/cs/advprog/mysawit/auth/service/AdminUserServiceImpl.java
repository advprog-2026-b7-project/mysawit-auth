package id.ac.ui.cs.advprog.mysawit.auth.service;
import id.ac.ui.cs.advprog.mysawit.auth.dto.AdminUserDetailResponse;
import id.ac.ui.cs.advprog.mysawit.auth.dto.AdminUserResponse;
import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser;
import id.ac.ui.cs.advprog.mysawit.auth.entity.Role;
import id.ac.ui.cs.advprog.mysawit.auth.repository.AssignmentRepository;
import id.ac.ui.cs.advprog.mysawit.auth.repository.AuthUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final AuthUserRepository userRepository;
    private final AssignmentRepository assignmentRepository;

    @Override
    public Page<AdminUserResponse> getUsers(
            String name, String email, Role role, Pageable pageable) {
        String nameFilter  = (name  == null || name.isBlank())  ? null : name.trim();
        String emailFilter = (email == null || email.isBlank()) ? null : email.trim();
        boolean hasFilters = nameFilter != null || emailFilter != null || role != null;
        Page<AuthUser> users = hasFilters
                ? userRepository.findUsersByFilters(nameFilter, emailFilter, role, pageable)
                : userRepository.findAll(pageable);
        return users
                .map(this::toResponse);
    }

    @Override
    public AdminUserDetailResponse getUserById(UUID userId) {
        AuthUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));
        return toDetailResponse(user);
    }

    @Override
    @Transactional
    public String deleteUser(UUID userId, UUID requesterId) {
        if (userId.equals(requesterId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Admin cannot delete their own account");
        }

        AuthUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        if (user.getRole() == Role.MANDOR) {
            if (assignmentRepository.existsByMandorAndPlantationIdIsNotNull(user)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Cannot delete mandor with active plantation assignments");
            }
            assignmentRepository.deleteAll(assignmentRepository.findByMandor(user));
            userRepository.clearMandorReference(user);

        } else if (user.getRole() == Role.BURUH) {
            if (assignmentRepository.existsByBuruhAndPlantationIdIsNotNull(user)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Cannot delete buruh with active plantation assignments");
            }
            assignmentRepository.deleteAll(assignmentRepository.findByBuruh(user));
        }

        String email = user.getEmail();
        userRepository.delete(user);
        return "User " + email + " successfully deleted.";
    }

    private AdminUserResponse toResponse(AuthUser user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nama(user.getNama())
                .email(user.getEmail())
                .role(user.getRole())
                .mandorCertificationNumber(user.getMandorCertificationNumber())
                .mandorId(user.getMandor() != null ? user.getMandor().getId() : null)
                .createdAt(user.getCreatedAt())
                .build();
    }

    private AdminUserDetailResponse toDetailResponse(AuthUser user) {
        // TODO: Populate kebunId and kebunNama after Plantation module integration.
        return AdminUserDetailResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nama(user.getNama())
                .email(user.getEmail())
                .role(user.getRole())
                .walletBalance(user.getWalletBalance() != null
                        ? user.getWalletBalance()
                        : BigDecimal.ZERO)
                .mandorCertificationNumber(user.getRole() == Role.MANDOR
                        ? user.getMandorCertificationNumber()
                        : null)
                .mandorId(user.getRole() == Role.BURUH && user.getMandor() != null
                        ? user.getMandor().getId()
                        : null)
                .kebunId(null)
                .kebunNama(null)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
