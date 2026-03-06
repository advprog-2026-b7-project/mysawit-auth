package id.ac.ui.cs.advprog.mysawit.auth.service;

import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser;
import id.ac.ui.cs.advprog.mysawit.auth.entity.Role;

import java.util.List;

public interface AdminUserService {
    List<AuthUser> getUsers(String name, String email, Role role);
}