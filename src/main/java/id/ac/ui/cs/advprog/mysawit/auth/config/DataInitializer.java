package id.ac.ui.cs.advprog.mysawit.auth.config;

import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser;
import id.ac.ui.cs.advprog.mysawit.auth.entity.Role;
import id.ac.ui.cs.advprog.mysawit.auth.repository.AuthUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final AuthUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email:admin@mysawit.com}")
    private String adminEmail;

    @Value("${admin.password:Admin1234!}")
    private String adminPassword;

    @Value("${admin.username:Admin}")
    private String adminUsername;

    @Override
    public void run(String... args) {
        AuthUser admin = userRepository.findByEmail(adminEmail).orElse(null);

        if (admin == null) {
            admin = AuthUser.builder()
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .username(adminUsername)
                    .role(Role.ADMIN)
                    .build();
            userRepository.save(admin);
            log.info("Admin account created: {}", adminEmail);
        } else {
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setUsername(adminUsername);
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
            log.info("Admin account synced: {}", adminEmail);
        }
    }
}
