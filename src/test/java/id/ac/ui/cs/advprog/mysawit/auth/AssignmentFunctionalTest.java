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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AssignmentFunctionalTest {

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

    private String registerAndGetId(String email, String role, String certNumber) {
        String uid = UUID.randomUUID().toString();
        Map<String, Object> body = new HashMap<>();
        body.put("username", role.toLowerCase() + "-" + uid);
        body.put("nama", role + " Test");
        body.put("email", email);
        body.put("password", "Pass1234!");
        body.put("role", role);
        if (certNumber != null) {
            body.put("mandorCertificationNumber", certNumber);
        }

        given()
            .contentType(ContentType.JSON)
            .body(body)
        .when()
            .post("/api/auth/register")
        .then()
            .statusCode(201);

        String token = given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", email, "password", "Pass1234!"))
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(200)
            .extract().path("token");

        return given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/api/auth/me")
        .then()
            .statusCode(200)
            .extract().path("id");
    }

    private String createAssignment(String adminToken, String buruhId, String mandorId) {
        return given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + adminToken)
            .body(Map.of("buruhId", buruhId, "mandorId", mandorId))
        .when()
            .post("/api/assignments")
        .then()
            .statusCode(201)
            .extract().path("data.id");
    }

    @Test
    void createAssignment_asAdmin_returns201() {
        String uid = UUID.randomUUID().toString();
        String mandorId = registerAndGetId(
            "mandor-" + uid + "@test.com", "MANDOR", "CERT-" + uid
        );
        String buruhId = registerAndGetId(
            "buruh-" + uid + "@test.com", "BURUH", null
        );
        String adminToken = loginAdmin();
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + adminToken)
            .body(Map.of("buruhId", buruhId, "mandorId", mandorId))
        .when()
            .post("/api/assignments")
        .then()
            .statusCode(201)
            .body("data.id", notNullValue());
    }

    @Test
    void listAssignments_asAdmin_returns200() {
        String uid = UUID.randomUUID().toString();
        String mandorId = registerAndGetId(
            "mandor-" + uid + "@test.com", "MANDOR", "CERT-" + uid
        );
        String buruhId = registerAndGetId(
            "buruh-" + uid + "@test.com", "BURUH", null
        );
        String adminToken = loginAdmin();
        createAssignment(adminToken, buruhId, mandorId);

        given()
            .header("Authorization", "Bearer " + adminToken)
        .when()
            .get("/api/assignments")
        .then()
            .statusCode(200)
            .body("data.content", notNullValue());
    }

    @Test
    void getAssignmentById_asAdmin_returns200() {
        String uid = UUID.randomUUID().toString();
        String mandorId = registerAndGetId(
            "mandor-" + uid + "@test.com", "MANDOR", "CERT-" + uid
        );
        String buruhId = registerAndGetId(
            "buruh-" + uid + "@test.com", "BURUH", null
        );
        String adminToken = loginAdmin();
        String assignmentId = createAssignment(adminToken, buruhId, mandorId);

        given()
            .header("Authorization", "Bearer " + adminToken)
        .when()
            .get("/api/assignments/" + assignmentId)
        .then()
            .statusCode(200);
    }

    @Test
    void getAssignmentByBuruhId_asAdmin_returns200() {
        String uid = UUID.randomUUID().toString();
        String mandorId = registerAndGetId(
            "mandor-" + uid + "@test.com", "MANDOR", "CERT-" + uid
        );
        String buruhId = registerAndGetId(
            "buruh-" + uid + "@test.com", "BURUH", null
        );
        String adminToken = loginAdmin();
        createAssignment(adminToken, buruhId, mandorId);

        given()
            .header("Authorization", "Bearer " + adminToken)
        .when()
            .get("/api/assignments/buruh/" + buruhId)
        .then()
            .statusCode(200);
    }

    @Test
    void deleteAssignment_asAdmin_returns200() {
        String uid = UUID.randomUUID().toString();
        String mandorId = registerAndGetId(
            "mandor-" + uid + "@test.com", "MANDOR", "CERT-" + uid
        );
        String buruhId = registerAndGetId(
            "buruh-" + uid + "@test.com", "BURUH", null
        );
        String adminToken = loginAdmin();
        String assignmentId = createAssignment(adminToken, buruhId, mandorId);

        given()
            .header("Authorization", "Bearer " + adminToken)
        .when()
            .delete("/api/assignments/" + assignmentId)
        .then()
            .statusCode(200);
    }

    @Test
    void reassignBuruh_asAdmin_returns200() {
        String uid = UUID.randomUUID().toString();
        String mandor1Id = registerAndGetId(
            "mandor1-" + uid + "@test.com", "MANDOR", "CERT1-" + uid
        );
        String mandor2Id = registerAndGetId(
            "mandor2-" + uid + "@test.com", "MANDOR", "CERT2-" + uid
        );
        String buruhId = registerAndGetId(
            "buruh-" + uid + "@test.com", "BURUH", null
        );
        String adminToken = loginAdmin();
        String assignmentId = createAssignment(adminToken, buruhId, mandor1Id);

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + adminToken)
            .body(Map.of("newMandorId", mandor2Id))
        .when()
            .post("/api/assignments/" + assignmentId + "/reassign")
        .then()
            .statusCode(200);
    }
}
