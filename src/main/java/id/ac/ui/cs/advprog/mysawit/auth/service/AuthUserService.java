package id.ac.ui.cs.advprog.mysawit.auth.service;

import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser;
import id.ac.ui.cs.advprog.mysawit.auth.repository.AuthUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthUserService {

    private final AuthUserRepository repository;

    public List<AuthUser> getAllUsers() {
        return repository.findAll();
    }

    public AuthUser createUser(AuthUser user) {
        return repository.save(user);
    }
}