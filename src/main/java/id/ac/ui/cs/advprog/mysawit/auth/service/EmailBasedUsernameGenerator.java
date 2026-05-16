package id.ac.ui.cs.advprog.mysawit.auth.service;

import id.ac.ui.cs.advprog.mysawit.auth.repository.AuthUserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EmailBasedUsernameGenerator implements UsernameGenerator {

    private final AuthUserRepository userRepository;

    @Override
    public String generate(String email) {
        String base = email.split("@")[0].replaceAll("[^a-zA-Z0-9_.]", "_");
        if (base.isBlank()) {
            return "user_" + UUID.randomUUID().toString().substring(0, 8);
        }
        if (!userRepository.existsByUsername(base)) {
            return base;
        }
        return base + "_" + UUID.randomUUID().toString().substring(0, 6);
    }
}
