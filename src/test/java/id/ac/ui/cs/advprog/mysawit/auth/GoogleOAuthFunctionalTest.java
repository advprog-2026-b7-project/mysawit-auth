package id.ac.ui.cs.advprog.mysawit.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import id.ac.ui.cs.advprog.mysawit.auth.repository.AssignmentRepository;
import id.ac.ui.cs.advprog.mysawit.auth.repository.AuthUserRepository;
import id.ac.ui.cs.advprog.mysawit.auth.service.GoogleTokenVerifier;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GoogleOAuthFunctionalTest {

    @LocalServerPort
    private int port;

    @Autowired
    private AuthUserRepository userRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @MockBean
    private GoogleTokenVerifier googleTokenVerifier;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        assignmentRepository.deleteAll();
        userRepository.findAll().stream()
            .filter(u -> !u.getEmail().equals("admin@mysawit.com"))
            .forEach(userRepository::delete);
    }

    private GoogleIdToken.Payload buildPayload(String email, String name) {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmail(email);
        payload.set("name", name);
        return payload;
    }

    @Test
    void googleLogin_existingUser_returns200() {
        String uid = UUID.randomUUID().toString();
        String email = "google-existing-" + uid + "@gmail.com";

        Map<String, Object> regBody = new HashMap<>();
        regBody.put("username", "google-" + uid);
        regBody.put("nama", "Google Test");
        regBody.put("email", email);
        regBody.put("password", "Pass1234!");
        regBody.put("role", "BURUH");
        given().contentType(ContentType.JSON).body(regBody)
            .when().post("/api/auth/register").then().statusCode(201);

        when(googleTokenVerifier.verify("valid-token")).thenReturn(buildPayload(email, "Google Test"));

        given()
            .contentType(ContentType.JSON)
            .body(Map.of("idToken", "valid-token"))
        .when()
            .post("/api/auth/google-login")
        .then()
            .statusCode(200)
            .body("token", notNullValue());
    }

    @Test
    void googleLogin_newBuruhUser_returns200_notExpected201() {
        String uid = UUID.randomUUID().toString();
        String email = "google-new-" + uid + "@gmail.com";

        when(googleTokenVerifier.verify("new-user-token")).thenReturn(buildPayload(email, "New User"));

        given()
            .contentType(ContentType.JSON)
            .body(Map.of("idToken", "new-user-token", "role", "BURUH"))
        .when()
            .post("/api/auth/google-login")
        .then()
            .statusCode(200)
            .body("token", notNullValue());
    }

    @Test
    void googleLogin_newMandorWithCert_returns200() {
        String uid = UUID.randomUUID().toString();
        String email = "google-mandor-" + uid + "@gmail.com";

        when(googleTokenVerifier.verify("mandor-token")).thenReturn(buildPayload(email, "Mandor Google"));

        given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "idToken", "mandor-token",
                "role", "MANDOR",
                "mandorCertificationNumber", "GCERT-" + uid
            ))
        .when()
            .post("/api/auth/google-login")
        .then()
            .statusCode(200)
            .body("token", notNullValue());
    }

    @Test
    void googleLogin_invalidToken_returns400() {
        when(googleTokenVerifier.verify("bad-token"))
            .thenThrow(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid or expired Google token"));

        given()
            .contentType(ContentType.JSON)
            .body(Map.of("idToken", "bad-token", "role", "BURUH"))
        .when()
            .post("/api/auth/google-login")
        .then()
            .statusCode(400);
    }

    @Test
    void googleLogin_missingIdToken_returns400() {
        given()
            .contentType(ContentType.JSON)
            .body("{}")
        .when()
            .post("/api/auth/google-login")
        .then()
            .statusCode(400);
    }

    @Test
    void googleLogin_newUserNoRole_returns400() {
        String uid = UUID.randomUUID().toString();
        String email = "google-norole-" + uid + "@gmail.com";

        when(googleTokenVerifier.verify("norole-token")).thenReturn(buildPayload(email, "No Role"));

        given()
            .contentType(ContentType.JSON)
            .body(Map.of("idToken", "norole-token"))
        .when()
            .post("/api/auth/google-login")
        .then()
            .statusCode(400);
    }

    @Test
    void googleLogin_newUserAdminRole_returns403() {
        String uid = UUID.randomUUID().toString();
        String email = "google-admin-" + uid + "@gmail.com";

        when(googleTokenVerifier.verify("admin-token")).thenReturn(buildPayload(email, "Admin Attempt"));

        given()
            .contentType(ContentType.JSON)
            .body(Map.of("idToken", "admin-token", "role", "ADMIN"))
        .when()
            .post("/api/auth/google-login")
        .then()
            .statusCode(403);
    }

    @Test
    void googleLogin_newMandorDuplicateCert_returns409() {
        String uid = UUID.randomUUID().toString();
        String cert = "GCERT-DUP-" + uid;

        Map<String, Object> mandorBody = new HashMap<>();
        mandorBody.put("username", "mandor-prior-" + uid);
        mandorBody.put("nama", "Prior Mandor");
        mandorBody.put("email", "mandorprior-" + uid + "@test.com");
        mandorBody.put("password", "Pass1234!");
        mandorBody.put("role", "MANDOR");
        mandorBody.put("mandorCertificationNumber", cert);
        given().contentType(ContentType.JSON).body(mandorBody)
            .when().post("/api/auth/register").then().statusCode(201);

        String email = "google-mandor-dup-" + uid + "@gmail.com";
        when(googleTokenVerifier.verify(anyString())).thenReturn(buildPayload(email, "Dup Mandor"));

        given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "idToken", "dup-cert-token",
                "role", "MANDOR",
                "mandorCertificationNumber", cert
            ))
        .when()
            .post("/api/auth/google-login")
        .then()
            .statusCode(409);
    }
}
