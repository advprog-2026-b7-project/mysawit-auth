package id.ac.ui.cs.advprog.mysawit.auth;

import id.ac.ui.cs.advprog.mysawit.auth.repository.AssignmentRepository;
import id.ac.ui.cs.advprog.mysawit.auth.repository.AuthUserRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthFunctionalTest {

    @LocalServerPort
    private int port;

    @Autowired
    private AuthUserRepository userRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        assignmentRepository.deleteAll();
        userRepository.findAll().stream()
            .filter(u -> !u.getEmail().equals("admin@mysawit.com"))
            .forEach(userRepository::delete);
    }

    private String loginAdmin() {
        return given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", "admin@mysawit.com", "password", "Admin1234!"))
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(200)
            .extract().path("token");
    }

    @Test
    void register_validBuruh_returns201() {
        String email = "buruh-" + UUID.randomUUID() + "@test.com";
        Map<String, Object> body = Map.of(
            "username", "buruh-" + UUID.randomUUID(),
            "nama", "Buruh Test",
            "email", email,
            "password", "Pass1234!",
            "role", "BURUH"
        );
        given()
            .contentType(ContentType.JSON)
            .body(body)
        .when()
            .post("/api/auth/register")
        .then()
            .statusCode(201)
            .body("token", notNullValue());
    }

    @Test
    void register_validMandor_returns201() {
        String uid = UUID.randomUUID().toString();
        Map<String, Object> body = Map.of(
            "username", "mandor-" + uid,
            "nama", "Mandor Test",
            "email", "mandor-" + uid + "@test.com",
            "password", "Pass1234!",
            "role", "MANDOR",
            "mandorCertificationNumber", "CERT-" + uid
        );
        given()
            .contentType(ContentType.JSON)
            .body(body)
        .when()
            .post("/api/auth/register")
        .then()
            .statusCode(201)
            .body("token", notNullValue());
    }

    @Test
    void register_duplicateEmail_returns409() {
        String uid = UUID.randomUUID().toString();
        Map<String, Object> body = Map.of(
            "username", "dup-" + uid,
            "nama", "Dup Test",
            "email", "dup-" + uid + "@test.com",
            "password", "Pass1234!",
            "role", "BURUH"
        );
        given()
            .contentType(ContentType.JSON)
            .body(body)
        .when()
            .post("/api/auth/register")
        .then()
            .statusCode(201);

        Map<String, Object> body2 = Map.of(
            "username", "dup2-" + uid,
            "nama", "Dup Test 2",
            "email", "dup-" + uid + "@test.com",
            "password", "Pass1234!",
            "role", "BURUH"
        );
        given()
            .contentType(ContentType.JSON)
            .body(body2)
        .when()
            .post("/api/auth/register")
        .then()
            .statusCode(409);
    }

    @Test
    void register_missingFields_returns400() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of())
        .when()
            .post("/api/auth/register")
        .then()
            .statusCode(400);
    }

    @Test
    void login_validAdminCredentials_returns200() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", "admin@mysawit.com", "password", "Admin1234!"))
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(200)
            .body("token", notNullValue());
    }

    @Test
    void login_wrongPassword_returns401() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", "admin@mysawit.com", "password", "wrongpass"))
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(401);
    }

    @Test
    void login_unknownEmail_returns401() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", "nobody@nowhere.com", "password", "Pass1234!"))
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(401);
    }

    @Test
    void getMe_withValidToken_returns200() {
        String token = loginAdmin();
        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/api/auth/me")
        .then()
            .statusCode(200)
            .body("email", equalTo("admin@mysawit.com"));
    }

    @Test
    void getMe_withoutToken_returns401() {
        given()
        .when()
            .get("/api/auth/me")
        .then()
            .statusCode(401);
    }

    @Test
    void getProfile_existingUser_returns200() {
        String token = loginAdmin();
        String id = given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/api/auth/me")
        .then()
            .statusCode(200)
            .extract().path("id");

        given()
        .when()
            .get("/api/auth/profile/" + id)
        .then()
            .statusCode(200);
    }
}
