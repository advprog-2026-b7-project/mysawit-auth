package id.ac.ui.cs.advprog.mysawit.auth.repository;

import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser;
import id.ac.ui.cs.advprog.mysawit.auth.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
class AuthUserRepositoryTest {

    @Autowired
    private AuthUserRepository authUserRepository;

    private static final Pageable DEFAULT_PAGE = PageRequest.of(0, 20);

    @Test
    void findUsersByFilters_shouldReturnAllUsers_whenFiltersAreNull() {
        AuthUser admin = createUser("Alice Admin", "alice@example.com", Role.ADMIN);
        AuthUser mandor = createUser("Bob Mandor", "bob@example.com", Role.MANDOR);

        authUserRepository.saveAll(java.util.List.of(admin, mandor));

        Page<AuthUser> users = authUserRepository.findUsersByFilters(
                null, null, null, DEFAULT_PAGE);

        assertEquals(2, users.getTotalElements());
    }

    @Test
    void findUsersByFilters_shouldFilterByNameEmailAndRole() {
        AuthUser target = createUser("Alice Admin", "alice@example.com", Role.ADMIN);
        AuthUser sameName = createUser("Alice Mandor", "alice.mandor@example.com", Role.MANDOR);
        AuthUser sameRole = createUser("Charlie Admin", "charlie@example.com", Role.ADMIN);

        authUserRepository.saveAll(java.util.List.of(target, sameName, sameRole));

        Page<AuthUser> users = authUserRepository.findUsersByFilters(
                "ALICE",
                "EXAMPLE.COM",
                Role.ADMIN,
                DEFAULT_PAGE
        );

        assertEquals(1, users.getTotalElements());
        assertEquals("alice@example.com", users.getContent().getFirst().getEmail());
    }

    @Test
    void findUsersByFilters_shouldFilterBySingleRole() {
        AuthUser admin = createUser("Alice Admin", "alice@example.com", Role.ADMIN);
        AuthUser mandor = createUser("Bob Mandor", "bob@example.com", Role.MANDOR);

        authUserRepository.saveAll(java.util.List.of(admin, mandor));

        Page<AuthUser> users = authUserRepository.findUsersByFilters(
                null, null, Role.MANDOR, DEFAULT_PAGE);

        assertEquals(1, users.getTotalElements());
        assertEquals("bob@example.com", users.getContent().getFirst().getEmail());
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
