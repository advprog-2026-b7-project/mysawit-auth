package id.ac.ui.cs.advprog.mysawit.auth.service;

import id.ac.ui.cs.advprog.mysawit.auth.repository.AuthUserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailBasedUsernameGeneratorTest {

    @Mock
    private AuthUserRepository userRepository;

    @InjectMocks
    private EmailBasedUsernameGenerator generator;

    @Test
    void generate_normalEmail_usernameAvailable_returnsBase() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);

        String result = generator.generate("alice@example.com");

        assertEquals("alice", result);
    }

    @Test
    void generate_usernameTaken_appendsSuffix() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        String result = generator.generate("alice@example.com");

        assertTrue(result.startsWith("alice_"));
        assertTrue(result.length() > "alice_".length());
    }

    @Test
    void generate_blankPrefixAfterCleaning_returnsUserPrefix() {
        String result = generator.generate("@example.com");

        assertTrue(result.startsWith("user_"));
        assertEquals(13, result.length());
    }

    @Test
    void generate_specialCharsInPrefix_replacedWithUnderscore() {
        when(userRepository.existsByUsername("hello_world")).thenReturn(false);

        String result = generator.generate("hello world@example.com");

        assertNotNull(result);
        assertEquals("hello_world", result);
    }
}
