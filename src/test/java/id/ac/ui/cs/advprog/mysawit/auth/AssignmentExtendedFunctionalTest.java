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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AssignmentExtendedFunctionalTest {

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

    private String registerAndGetToken(String email, String role, String certNumber) {
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
        given().contentType(ContentType.JSON).body(body)
            .when().post("/api/auth/register").then().statusCode(201);

        return given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", email, "password", "Pass1234!"))
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(200)
            .extract().path("token");
    }

    private String getIdFromToken(String token) {
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
    void createAssignment_duplicateBuruh_returns409() {
        String uid = UUID.randomUUID().toString();
        String adminToken = loginAdmin();
        String mandorToken = registerAndGetToken(
            "mandor-" + uid + "@test.com", "MANDOR", "CERT-" + uid);
        String buruhToken = registerAndGetToken(
            "buruh-" + uid + "@test.com", "BURUH", null);

        String mandorId = getIdFromToken(mandorToken);
        String buruhId = getIdFromToken(buruhToken);

        createAssignment(adminToken, buruhId, mandorId);

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + adminToken)
            .body(Map.of("buruhId", buruhId, "mandorId", mandorId))
        .when()
            .post("/api/assignments")
        .then()
            .statusCode(409);
    }

    @Test
    void createAssignment_asBuruh_returns403() {
        String uid = UUID.randomUUID().toString();
        String adminToken = loginAdmin();
        String mandorToken = registerAndGetToken(
            "mandor-" + uid + "@test.com", "MANDOR", "CERT-" + uid);
        String buruhToken = registerAndGetToken(
            "buruh-" + uid + "@test.com", "BURUH", null);

        String mandorId = getIdFromToken(mandorToken);
        String buruhId = getIdFromToken(buruhToken);

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + buruhToken)
            .body(Map.of("buruhId", buruhId, "mandorId", mandorId))
        .when()
            .post("/api/assignments")
        .then()
            .statusCode(403);
    }

    @Test
    void getAllAssignments_buruhSeesOnlyOwnAssignment() {
        String uid = UUID.randomUUID().toString();
        String adminToken = loginAdmin();
        String mandorToken = registerAndGetToken(
            "mandor-" + uid + "@test.com", "MANDOR", "CERT-" + uid);
        String buruhToken = registerAndGetToken(
            "buruh-" + uid + "@test.com", "BURUH", null);

        String mandorId = getIdFromToken(mandorToken);
        String buruhId = getIdFromToken(buruhToken);
        createAssignment(adminToken, buruhId, mandorId);

        given()
            .header("Authorization", "Bearer " + buruhToken)
        .when()
            .get("/api/assignments")
        .then()
            .statusCode(200)
            .body("data.content", hasSize(1))
            .body("data.content[0].buruhId", equalTo(buruhId));
    }

    @Test
    void getAllAssignments_mandorSeesOnlyOwnBuruh() {
        String uid = UUID.randomUUID().toString();
        String adminToken = loginAdmin();
        String mandorToken = registerAndGetToken(
            "mandor-" + uid + "@test.com", "MANDOR", "CERT-" + uid);
        String buruhToken = registerAndGetToken(
            "buruh-" + uid + "@test.com", "BURUH", null);

        String mandorId = getIdFromToken(mandorToken);
        String buruhId = getIdFromToken(buruhToken);
        createAssignment(adminToken, buruhId, mandorId);

        given()
            .header("Authorization", "Bearer " + mandorToken)
        .when()
            .get("/api/assignments")
        .then()
            .statusCode(200)
            .body("data.content", hasSize(1))
            .body("data.content[0].mandorId", equalTo(mandorId));
    }

    @Test
    void getAssignmentById_buruhAccessingOthers_returns403() {
        String uid = UUID.randomUUID().toString();
        String adminToken = loginAdmin();
        String mandorToken = registerAndGetToken(
            "mandor-" + uid + "@test.com", "MANDOR", "CERT-" + uid);
        String buruh1Token = registerAndGetToken(
            "buruh1-" + uid + "@test.com", "BURUH", null);
        String buruh2Token = registerAndGetToken(
            "buruh2-" + uid + "@test.com", "BURUH", null);

        String mandorId = getIdFromToken(mandorToken);
        String buruh1Id = getIdFromToken(buruh1Token);
        String assignmentId = createAssignment(adminToken, buruh1Id, mandorId);

        given()
            .header("Authorization", "Bearer " + buruh2Token)
        .when()
            .get("/api/assignments/" + assignmentId)
        .then()
            .statusCode(403);
    }

    @Test
    void reassignBuruh_updatesReassignedAt() {
        String uid = UUID.randomUUID().toString();
        String adminToken = loginAdmin();
        String mandor1Token = registerAndGetToken(
            "mandor1-" + uid + "@test.com", "MANDOR", "CERT1-" + uid);
        String mandor2Token = registerAndGetToken(
            "mandor2-" + uid + "@test.com", "MANDOR", "CERT2-" + uid);
        String buruhToken = registerAndGetToken(
            "buruh-" + uid + "@test.com", "BURUH", null);

        String mandor1Id = getIdFromToken(mandor1Token);
        String mandor2Id = getIdFromToken(mandor2Token);
        String buruhId = getIdFromToken(buruhToken);
        String assignmentId = createAssignment(adminToken, buruhId, mandor1Id);

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + adminToken)
            .body(Map.of("newMandorId", mandor2Id))
        .when()
            .post("/api/assignments/" + assignmentId + "/reassign")
        .then()
            .statusCode(200)
            .body("data.reassignedAt", notNullValue())
            .body("data.mandorId", equalTo(mandor2Id));
    }

    @Test
    void reassignBuruh_sameMandor_returns400() {
        String uid = UUID.randomUUID().toString();
        String adminToken = loginAdmin();
        String mandorToken = registerAndGetToken(
            "mandor-" + uid + "@test.com", "MANDOR", "CERT-" + uid);
        String buruhToken = registerAndGetToken(
            "buruh-" + uid + "@test.com", "BURUH", null);

        String mandorId = getIdFromToken(mandorToken);
        String buruhId = getIdFromToken(buruhToken);
        String assignmentId = createAssignment(adminToken, buruhId, mandorId);

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + adminToken)
            .body(Map.of("newMandorId", mandorId))
        .when()
            .post("/api/assignments/" + assignmentId + "/reassign")
        .then()
            .statusCode(400);
    }

    @Test
    void deleteAssignment_byBuruhTheirOwn_returns200() {
        String uid = UUID.randomUUID().toString();
        String adminToken = loginAdmin();
        String mandorToken = registerAndGetToken(
            "mandor-" + uid + "@test.com", "MANDOR", "CERT-" + uid);
        String buruhToken = registerAndGetToken(
            "buruh-" + uid + "@test.com", "BURUH", null);

        String mandorId = getIdFromToken(mandorToken);
        String buruhId = getIdFromToken(buruhToken);
        String assignmentId = createAssignment(adminToken, buruhId, mandorId);

        given()
            .header("Authorization", "Bearer " + buruhToken)
        .when()
            .delete("/api/assignments/" + assignmentId)
        .then()
            .statusCode(200);
    }

    @Test
    void deleteAssignment_byBuruhAccessingOthers_returns403() {
        String uid = UUID.randomUUID().toString();
        String adminToken = loginAdmin();
        String mandorToken = registerAndGetToken(
            "mandor-" + uid + "@test.com", "MANDOR", "CERT-" + uid);
        String buruh1Token = registerAndGetToken(
            "buruh1-" + uid + "@test.com", "BURUH", null);
        String buruh2Token = registerAndGetToken(
            "buruh2-" + uid + "@test.com", "BURUH", null);

        String mandorId = getIdFromToken(mandorToken);
        String buruh1Id = getIdFromToken(buruh1Token);
        String assignmentId = createAssignment(adminToken, buruh1Id, mandorId);

        given()
            .header("Authorization", "Bearer " + buruh2Token)
        .when()
            .delete("/api/assignments/" + assignmentId)
        .then()
            .statusCode(403);
    }

    @Test
    void getMandorAssignments_mandorAccessingOwn_returns200() {
        String uid = UUID.randomUUID().toString();
        String adminToken = loginAdmin();
        String mandorToken = registerAndGetToken(
            "mandor-" + uid + "@test.com", "MANDOR", "CERT-" + uid);
        String buruhToken = registerAndGetToken(
            "buruh-" + uid + "@test.com", "BURUH", null);

        String mandorId = getIdFromToken(mandorToken);
        String buruhId = getIdFromToken(buruhToken);
        createAssignment(adminToken, buruhId, mandorId);

        given()
            .header("Authorization", "Bearer " + mandorToken)
        .when()
            .get("/api/assignments/mandor/" + mandorId)
        .then()
            .statusCode(200)
            .body("data.content", hasSize(1));
    }

    @Test
    void getMandorAssignments_mandorAccessingOthers_returns403() {
        String uid = UUID.randomUUID().toString();
        String adminToken = loginAdmin();
        String mandor1Token = registerAndGetToken(
            "mandor1-" + uid + "@test.com", "MANDOR", "CERT1-" + uid);
        String mandor2Token = registerAndGetToken(
            "mandor2-" + uid + "@test.com", "MANDOR", "CERT2-" + uid);
        String buruhToken = registerAndGetToken(
            "buruh-" + uid + "@test.com", "BURUH", null);

        String mandor1Id = getIdFromToken(mandor1Token);
        String buruhId = getIdFromToken(buruhToken);
        createAssignment(adminToken, buruhId, mandor1Id);

        String mandor2Id = getIdFromToken(mandor2Token);

        given()
            .header("Authorization", "Bearer " + mandor2Token)
        .when()
            .get("/api/assignments/mandor/" + mandor1Id)
        .then()
            .statusCode(403);
    }
}
