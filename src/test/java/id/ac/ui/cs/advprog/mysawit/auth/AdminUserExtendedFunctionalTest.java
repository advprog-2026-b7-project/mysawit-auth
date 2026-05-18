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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AdminUserExtendedFunctionalTest {

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

    private String registerUser(String email, String role, String certNumber) {
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

    @Test
    void deleteUser_adminSelfDelete_returns403() {
        String adminToken = loginAdmin();
        String adminId = given()
            .header("Authorization", "Bearer " + adminToken)
        .when()
            .get("/api/auth/me")
        .then()
            .statusCode(200)
            .extract().path("id");

        given()
            .header("Authorization", "Bearer " + adminToken)
        .when()
            .delete("/api/admin/users/" + adminId)
        .then()
            .statusCode(403);
    }

    @Test
    void getUsers_withNameFilter_returnsFilteredResults() {
        String uid = UUID.randomUUID().toString();
        String adminToken = loginAdmin();

        String email = "filtername-" + uid + "@test.com";
        Map<String, Object> body = new HashMap<>();
        body.put("username", "filtername-" + uid);
        body.put("nama", "UniqueFilterName-" + uid);
        body.put("email", email);
        body.put("password", "Pass1234!");
        body.put("role", "BURUH");
        given().contentType(ContentType.JSON).body(body)
            .when().post("/api/auth/register").then().statusCode(201);

        given()
            .header("Authorization", "Bearer " + adminToken)
            .param("name", "UniqueFilterName-" + uid)
        .when()
            .get("/api/admin/users")
        .then()
            .statusCode(200)
            .body("data.content[0].email", equalTo(email));
    }

    @Test
    void getUsers_withRoleFilter_returnsOnlyMatchingRole() {
        String uid = UUID.randomUUID().toString();
        String adminToken = loginAdmin();

        registerUser("mandorfilter-" + uid + "@test.com", "MANDOR", "CERT-" + uid);

        given()
            .header("Authorization", "Bearer " + adminToken)
            .param("role", "MANDOR")
        .when()
            .get("/api/admin/users")
        .then()
            .statusCode(200)
            .body("data.content[0].role", equalTo("MANDOR"));
    }

    @Test
    void getUsers_withEmailFilter_returnsFilteredResults() {
        String uid = UUID.randomUUID().toString();
        String adminToken = loginAdmin();

        String email = "emailfilter-" + uid + "@uniquedomain.com";
        Map<String, Object> body = new HashMap<>();
        body.put("username", "emailfilter-" + uid);
        body.put("nama", "Email Filter Test");
        body.put("email", email);
        body.put("password", "Pass1234!");
        body.put("role", "BURUH");
        given().contentType(ContentType.JSON).body(body)
            .when().post("/api/auth/register").then().statusCode(201);

        given()
            .header("Authorization", "Bearer " + adminToken)
            .param("email", "uniquedomain.com")
        .when()
            .get("/api/admin/users")
        .then()
            .statusCode(200)
            .body("data.totalElements", notNullValue())
            .body("data.content[0].email", equalTo(email));
    }

    @Test
    void getUsers_pagination_returns200() {
        String adminToken = loginAdmin();

        given()
            .header("Authorization", "Bearer " + adminToken)
            .param("page", "0")
            .param("size", "5")
        .when()
            .get("/api/admin/users")
        .then()
            .statusCode(200)
            .body("data.size", equalTo(5));
    }
}
