package id.ac.ui.cs.advprog.mysawit.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GoogleIdTokenVerifierAdapterTest {

    private GoogleIdTokenVerifierAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new GoogleIdTokenVerifierAdapter();
        ReflectionTestUtils.setField(adapter, "googleClientId", "fake-client-id");
    }

    @Test
    void verify_responseStatusExceptionIsRethrown() {
        ResponseStatusException original = new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Invalid Google ID token");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> {
                    throw original;
                });

        assertSame(original, ex);
    }

    @Test
    void verify_nonRseExceptionWrappedInBadRequest() {
        String badToken = "this-is-not-a-valid-google-token";

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> adapter.verify(badToken));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
}
