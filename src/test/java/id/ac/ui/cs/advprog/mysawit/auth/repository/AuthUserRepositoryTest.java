package id.ac.ui.cs.advprog.mysawit.auth.repository;

import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser;
import id.ac.ui.cs.advprog.mysawit.auth.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
class AuthUserRepositoryTest {

    @Autowired
    private AuthUserRepository authUserRepository;

    @Test
    void findUsersByFilters_shouldReturnAllUsers_whenFiltersAreNull() {
        AuthUser admin = createUser("Alice Admin", "alice@example.com", Role.ADMIN);
        AuthUser mandor = createUser("Bob Mandor", "bob@example.com", Role.MANDOR);

        authUserRepository.saveAll(List.of(admin, mandor));

        List<AuthUser> users = authUserRepository.findUsersByFilters(null, null, null);

        assertEquals(2, users.size());
    }

    @Test
    void findUsersByFilters_shouldFilterByNameEmailAndRole() {
        AuthUser target = createUser("Alice Admin", "alice@example.com", Role.ADMIN);
        AuthUser sameName = createUser("Alice Mandor", "alice.mandor@example.com", Role.MANDOR);
        AuthUser sameRole = createUser("Charlie Admin", "charlie@example.com", Role.ADMIN);

        authUserRepository.saveAll(List.of(target, sameName, sameRole));

        List<AuthUser> users = authUserRepository.findUsersByFilters(
                "ALICE",
                "EXAMPLE.COM",
                Role.ADMIN
        );

        assertEquals(1, users.size());
        assertEquals("alice@example.com", users.getFirst().getEmail());
    }

    @Test
    void findUsersByFilters_shouldFilterBySingleRole() {
        AuthUser admin = createUser("Alice Admin", "alice@example.com", Role.ADMIN);
        AuthUser mandor = createUser("Bob Mandor", "bob@example.com", Role.MANDOR);

        authUserRepository.saveAll(List.of(admin, mandor));

        List<AuthUser> users = authUserRepository.findUsersByFilters(null, null, Role.MANDOR);

        assertEquals(1, users.size());
        assertEquals("bob@example.com", users.getFirst().getEmail());
    }

    private AuthUser createUser(String username, String email, Role role) {
        return AuthUser.builder()
                .username(username)
                .email(email)
                .role(role)
                .password("password")
                .build();
    }
}
