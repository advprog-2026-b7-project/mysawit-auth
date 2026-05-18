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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AdminUserFunctionalTest {

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

    private String registerAndLogin(String uid, String role) {
        Map<String, Object> body = Map.of(
            "username", role.toLowerCase() + "-" + uid,
            "nama", role + " Test",
            "email", role.toLowerCase() + "-" + uid + "@test.com",
            "password", "Pass1234!",
            "role", role
        );
        return given()
            .contentType(ContentType.JSON)
            .body(body)
        .when()
            .post("/api/auth/register")
        .then()
            .statusCode(201)
            .extract().path("token");
    }

    @Test
    void getUsers_asAdmin_returns200WithPaginatedData() {
        String token = loginAdmin();
        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/api/admin/users")
        .then()
            .statusCode(200)
            .body("data.content", org.hamcrest.Matchers.notNullValue());
    }

    @Test
    void getUsers_withBuruhToken_returns403() {
        String uid = UUID.randomUUID().toString();
        String buruhToken = registerAndLogin(uid, "BURUH");
        given()
            .header("Authorization", "Bearer " + buruhToken)
        .when()
            .get("/api/admin/users")
        .then()
            .statusCode(403);
    }

    @Test
    void getUsers_withoutToken_returns401() {
        given()
        .when()
            .get("/api/admin/users")
        .then()
            .statusCode(401);
    }

    @Test
    void deleteUser_asAdmin_returns200() {
        String uid = UUID.randomUUID().toString();
        String email = "buruh-" + uid + "@test.com";
        Map<String, Object> body = Map.of(
            "username", "buruh-" + uid,
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
            .statusCode(201);

        String buruhToken = given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", email, "password", "Pass1234!"))
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(200)
            .extract().path("token");

        String buruhId = given()
            .header("Authorization", "Bearer " + buruhToken)
        .when()
            .get("/api/auth/me")
        .then()
            .statusCode(200)
            .extract().path("id");

        String adminToken = loginAdmin();
        given()
            .header("Authorization", "Bearer " + adminToken)
        .when()
            .delete("/api/admin/users/" + buruhId)
        .then()
            .statusCode(200);
    }
}
