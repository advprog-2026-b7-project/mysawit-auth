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
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthExtendedFunctionalTest {

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
    void register_adminRole_returns403() {
        String uid = UUID.randomUUID().toString();
        Map<String, Object> body = new HashMap<>();
        body.put("username", "admin-" + uid);
        body.put("nama", "Admin Attempt");
        body.put("email", "adminattempt-" + uid + "@test.com");
        body.put("password", "Pass1234!");
        body.put("role", "ADMIN");

        given()
            .contentType(ContentType.JSON)
            .body(body)
        .when()
            .post("/api/auth/register")
        .then()
            .statusCode(403);
    }

    @Test
    void register_weakPassword_returns400() {
        String uid = UUID.randomUUID().toString();
        Map<String, Object> body = new HashMap<>();
        body.put("username", "buruh-" + uid);
        body.put("nama", "Buruh Test");
        body.put("email", "buruh-" + uid + "@test.com");
        body.put("password", "weak");
        body.put("role", "BURUH");

        given()
            .contentType(ContentType.JSON)
            .body(body)
        .when()
            .post("/api/auth/register")
        .then()
            .statusCode(400);
    }

    @Test
    void register_supirRole_returns201() {
        String uid = UUID.randomUUID().toString();
        Map<String, Object> body = new HashMap<>();
        body.put("username", "supir-" + uid);
        body.put("nama", "Supir Test");
        body.put("email", "supir-" + uid + "@test.com");
        body.put("password", "Pass1234!");
        body.put("role", "SUPIR");

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
    void register_mandorWithoutCert_returns400() {
        String uid = UUID.randomUUID().toString();
        Map<String, Object> body = new HashMap<>();
        body.put("username", "mandor-" + uid);
        body.put("nama", "Mandor Test");
        body.put("email", "mandor-" + uid + "@test.com");
        body.put("password", "Pass1234!");
        body.put("role", "MANDOR");

        given()
            .contentType(ContentType.JSON)
            .body(body)
        .when()
            .post("/api/auth/register")
        .then()
            .statusCode(400);
    }

    @Test
    void register_duplicateUsername_returns409() {
        String uid = UUID.randomUUID().toString();
        String username = "dupuser-" + uid;

        Map<String, Object> body1 = new HashMap<>();
        body1.put("username", username);
        body1.put("nama", "User One");
        body1.put("email", "user1-" + uid + "@test.com");
        body1.put("password", "Pass1234!");
        body1.put("role", "BURUH");

        given()
            .contentType(ContentType.JSON)
            .body(body1)
        .when()
            .post("/api/auth/register")
        .then()
            .statusCode(201);

        Map<String, Object> body2 = new HashMap<>();
        body2.put("username", username);
        body2.put("nama", "User Two");
        body2.put("email", "user2-" + uid + "@test.com");
        body2.put("password", "Pass1234!");
        body2.put("role", "BURUH");

        given()
            .contentType(ContentType.JSON)
            .body(body2)
        .when()
            .post("/api/auth/register")
        .then()
            .statusCode(409);
    }

    @Test
    void register_mandorDuplicateCert_returns409() {
        String uid = UUID.randomUUID().toString();
        String cert = "CERT-" + uid;

        Map<String, Object> body1 = new HashMap<>();
        body1.put("username", "mandor1-" + uid);
        body1.put("nama", "Mandor One");
        body1.put("email", "mandor1-" + uid + "@test.com");
        body1.put("password", "Pass1234!");
        body1.put("role", "MANDOR");
        body1.put("mandorCertificationNumber", cert);

        given()
            .contentType(ContentType.JSON)
            .body(body1)
        .when()
            .post("/api/auth/register")
        .then()
            .statusCode(201);

        Map<String, Object> body2 = new HashMap<>();
        body2.put("username", "mandor2-" + uid);
        body2.put("nama", "Mandor Two");
        body2.put("email", "mandor2-" + uid + "@test.com");
        body2.put("password", "Pass1234!");
        body2.put("role", "MANDOR");
        body2.put("mandorCertificationNumber", cert);

        given()
            .contentType(ContentType.JSON)
            .body(body2)
        .when()
            .post("/api/auth/register")
        .then()
            .statusCode(409);
    }

    @Test
    void login_wrongEmail_and_wrongPassword_return_same_401_message() {
        String wrongEmailResp = given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", "nobody@nowhere.com", "password", "Pass1234!"))
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(401)
            .extract().path("message");

        String wrongPasswordResp = given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", "admin@mysawit.com", "password", "wrongpass"))
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(401)
            .extract().path("message");

        org.hamcrest.MatcherAssert.assertThat(wrongEmailResp, equalTo(wrongPasswordResp));
    }

    @Test
    void logout_withValidToken_returns200() {
        String token = loginAdmin();

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(Map.of("token", token))
        .when()
            .post("/api/auth/logout")
        .then()
            .statusCode(200)
            .body("message", equalTo("Logout successful"));
    }

    @Test
    void getMe_mandorIdIsNull_afterAssignment_documentsBug() {
        String uid = UUID.randomUUID().toString();
        String adminToken = loginAdmin();

        Map<String, Object> mandorBody = new HashMap<>();
        mandorBody.put("username", "mandor-" + uid);
        mandorBody.put("nama", "Mandor Test");
        mandorBody.put("email", "mandor-" + uid + "@test.com");
        mandorBody.put("password", "Pass1234!");
        mandorBody.put("role", "MANDOR");
        mandorBody.put("mandorCertificationNumber", "CERT-" + uid);

        given().contentType(ContentType.JSON).body(mandorBody)
            .when().post("/api/auth/register").then().statusCode(201);

        String mandorToken = given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", "mandor-" + uid + "@test.com", "password", "Pass1234!"))
            .when().post("/api/auth/login").then().statusCode(200)
            .extract().path("token");

        String mandorId = given().header("Authorization", "Bearer " + mandorToken)
            .when().get("/api/auth/me").then().statusCode(200)
            .extract().path("id");

        Map<String, Object> buruhBody = new HashMap<>();
        buruhBody.put("username", "buruh-" + uid);
        buruhBody.put("nama", "Buruh Test");
        buruhBody.put("email", "buruh-" + uid + "@test.com");
        buruhBody.put("password", "Pass1234!");
        buruhBody.put("role", "BURUH");

        given().contentType(ContentType.JSON).body(buruhBody)
            .when().post("/api/auth/register").then().statusCode(201);

        String buruhToken = given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", "buruh-" + uid + "@test.com", "password", "Pass1234!"))
            .when().post("/api/auth/login").then().statusCode(200)
            .extract().path("token");

        String buruhId = given().header("Authorization", "Bearer " + buruhToken)
            .when().get("/api/auth/me").then().statusCode(200)
            .extract().path("id");

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + adminToken)
            .body(Map.of("buruhId", buruhId, "mandorId", mandorId))
        .when()
            .post("/api/assignments")
        .then()
            .statusCode(201);

        given()
            .header("Authorization", "Bearer " + buruhToken)
        .when()
            .get("/api/auth/me")
        .then()
            .statusCode(200)
            .body("mandorId", nullValue());
    }

    @Test
    void getProfile_unknownUser_returns404() {
        given()
        .when()
            .get("/api/auth/profile/" + UUID.randomUUID())
        .then()
            .statusCode(404);
    }

    @Test
    void getProfile_existingUser_returns200() {
        String token = loginAdmin();
        String adminId = given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/api/auth/me")
        .then()
            .statusCode(200)
            .extract().path("id");

        given()
        .when()
            .get("/api/auth/profile/" + adminId)
        .then()
            .statusCode(200)
            .body("email", equalTo("admin@mysawit.com"));
    }

    @Test
    void updateMe_updateNama_returns200() {
        String uid = UUID.randomUUID().toString();
        Map<String, Object> body = new HashMap<>();
        body.put("username", "upd-" + uid);
        body.put("nama", "Original Name");
        body.put("email", "upd-" + uid + "@test.com");
        body.put("password", "Pass1234!");
        body.put("role", "BURUH");
        String token = given().contentType(ContentType.JSON).body(body)
            .when().post("/api/auth/register").then().statusCode(201)
            .extract().path("token");

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(Map.of("nama", "Updated Name"))
        .when()
            .patch("/api/auth/me")
        .then()
            .statusCode(200)
            .body("nama", equalTo("Updated Name"));
    }

    @Test
    void updateMe_changePassword_returns200() {
        String uid = UUID.randomUUID().toString();
        Map<String, Object> body = new HashMap<>();
        body.put("username", "pwch-" + uid);
        body.put("nama", "PW Change");
        body.put("email", "pwch-" + uid + "@test.com");
        body.put("password", "Pass1234!");
        body.put("role", "BURUH");
        String token = given().contentType(ContentType.JSON).body(body)
            .when().post("/api/auth/register").then().statusCode(201)
            .extract().path("token");

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(Map.of("currentPassword", "Pass1234!", "newPassword", "NewPass1!"))
        .when()
            .patch("/api/auth/me")
        .then()
            .statusCode(200);

        given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", "pwch-" + uid + "@test.com", "password", "NewPass1!"))
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(200)
            .body("token", notNullValue());
    }

    @Test
    void updateMe_wrongCurrentPassword_returns400() {
        String uid = UUID.randomUUID().toString();
        Map<String, Object> body = new HashMap<>();
        body.put("username", "wrongpw-" + uid);
        body.put("nama", "Wrong PW");
        body.put("email", "wrongpw-" + uid + "@test.com");
        body.put("password", "Pass1234!");
        body.put("role", "BURUH");
        String token = given().contentType(ContentType.JSON).body(body)
            .when().post("/api/auth/register").then().statusCode(201)
            .extract().path("token");

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(Map.of("currentPassword", "WrongPass1!", "newPassword", "NewPass1!"))
        .when()
            .patch("/api/auth/me")
        .then()
            .statusCode(400);
    }

    @Test
    void updateMe_emailFieldPresent_returns400() {
        String token = loginAdmin();

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(Map.of("email", "newemail@test.com"))
        .when()
            .patch("/api/auth/me")
        .then()
            .statusCode(400);
    }

    @Test
    void updateMe_duplicateUsername_returns409() {
        String uid = UUID.randomUUID().toString();

        Map<String, Object> body1 = new HashMap<>();
        body1.put("username", "taken-" + uid);
        body1.put("nama", "User One");
        body1.put("email", "user1-" + uid + "@test.com");
        body1.put("password", "Pass1234!");
        body1.put("role", "BURUH");
        given().contentType(ContentType.JSON).body(body1)
            .when().post("/api/auth/register").then().statusCode(201);

        Map<String, Object> body2 = new HashMap<>();
        body2.put("username", "updater-" + uid);
        body2.put("nama", "User Two");
        body2.put("email", "user2-" + uid + "@test.com");
        body2.put("password", "Pass1234!");
        body2.put("role", "BURUH");
        String token2 = given().contentType(ContentType.JSON).body(body2)
            .when().post("/api/auth/register").then().statusCode(201)
            .extract().path("token");

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token2)
            .body(Map.of("username", "taken-" + uid))
        .when()
            .patch("/api/auth/me")
        .then()
            .statusCode(409);
    }

    @Test
    void updateMe_withoutToken_returns401() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("nama", "Name"))
        .when()
            .patch("/api/auth/me")
        .then()
            .statusCode(401);
    }
}
