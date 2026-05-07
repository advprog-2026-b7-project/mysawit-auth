package id.ac.ui.cs.advprog.mysawit.auth.service;
import id.ac.ui.cs.advprog.mysawit.auth.dto.AdminUserResponse;
import id.ac.ui.cs.advprog.mysawit.auth.entity.Role;

import java.util.List;
import java.util.UUID;

public interface AdminUserService {
    List<AdminUserResponse> getUsers(String name, String email, Role role);
    void deleteUser(UUID userId, UUID requesterId);
}