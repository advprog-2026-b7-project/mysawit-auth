package id.ac.ui.cs.advprog.mysawit.auth.controller;

import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser;
import id.ac.ui.cs.advprog.mysawit.auth.repository.AuthUserRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class AuthUserController {

    private final AuthUserRepository repository;

    public AuthUserController(AuthUserRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<AuthUser> getAllUsers() {
        return repository.findAll();
    }
}