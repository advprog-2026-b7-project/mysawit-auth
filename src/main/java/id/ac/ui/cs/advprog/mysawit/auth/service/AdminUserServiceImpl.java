package id.ac.ui.cs.advprog.mysawit.auth.service;


import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser;
import id.ac.ui.cs.advprog.mysawit.auth.repository.AuthUserRepository;
import id.ac.ui.cs.advprog.mysawit.auth.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import id.ac.ui.cs.advprog.mysawit.auth.entity.Role;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final AuthUserRepository userRepository;

    @Override
    public List<AuthUser> getUsers(String name, String email, Role role) {
        String normalizedName = normalize(name);
        String normalizedEmail = normalize(email);

        return userRepository.findUsersByFilters(normalizedName, normalizedEmail, role);
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}