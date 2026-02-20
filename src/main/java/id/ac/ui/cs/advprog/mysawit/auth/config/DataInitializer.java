package id.ac.ui.cs.advprog.mysawit.auth.config;

import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser;
import id.ac.ui.cs.advprog.mysawit.auth.repository.AuthUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final AuthUserRepository repository;

    public DataInitializer(AuthUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() == 0) {
            AuthUser user = AuthUser.builder()
                    .username("dummydata")
                    .build();
            repository.save(user);
        }
    }
}