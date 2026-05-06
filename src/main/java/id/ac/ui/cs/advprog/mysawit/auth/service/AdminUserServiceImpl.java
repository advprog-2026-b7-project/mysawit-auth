package id.ac.ui.cs.advprog.mysawit.auth.service;

import id.ac.ui.cs.advprog.mysawit.auth.dto.AdminUserResponse;

import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser;
import id.ac.ui.cs.advprog.mysawit.auth.entity.Role;
import id.ac.ui.cs.advprog.mysawit.auth.repository.AuthUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final AuthUserRepository userRepository;

    @Override
    public List<AdminUserResponse> getUsers(String name, String email, Role role) {
        String normalizedName = normalize(name);
        String normalizedEmail = normalize(email);
        return userRepository.findUsersByFilters(normalizedName, normalizedEmail, role)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public void deleteUser(UUID userId, UUID requesterId) {
        if (userId.equals(requesterId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Admin cannot delete their own account"
            );
        }

        AuthUser userToDelete = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        userRepository.delete(userToDelete);
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private AdminUserResponse toResponse(AuthUser user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .mandorCertificationNumber(user.getMandorCertificationNumber())
                .mandorId(user.getMandor() != null ? user.getMandor().getId() : null)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}