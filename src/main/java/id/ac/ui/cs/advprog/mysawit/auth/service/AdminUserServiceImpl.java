package id.ac.ui.cs.advprog.mysawit.auth.service;


import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser;
import id.ac.ui.cs.advprog.mysawit.auth.repository.AuthUserRepository;
import id.ac.ui.cs.advprog.mysawit.auth.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import id.ac.ui.cs.advprog.mysawit.auth.entity.Role;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final AuthUserRepository userRepository;

    @Override
    public List<AuthUser> getUsers(String name, String email, Role role) {
        if (name != null && email != null) {
            return userRepository
                    .findByUsernameContainingIgnoreCaseAndEmailContainingIgnoreCase(name, email);
        }
        if (name != null) {
            return userRepository
                    .findByUsernameContainingIgnoreCase(name);
        }
        if (email != null) {
            return userRepository
                    .findByEmailContainingIgnoreCase(email);
        }
        if (role != null) {
            return userRepository
                    .findByRole(role);
        }
        return userRepository.findAll();
    }
}