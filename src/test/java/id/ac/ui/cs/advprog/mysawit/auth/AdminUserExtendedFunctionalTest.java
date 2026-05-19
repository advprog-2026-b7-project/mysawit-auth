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

    @Test
    void getUserById_asAdmin_returns200WithDetailFields() {
        String uid = UUID.randomUUID().toString();
        String adminToken = loginAdmin();

        String email = "buruh-detail-" + uid + "@test.com";
        Map<String, Object> body = new HashMap<>();
        body.put("username", "buruh-detail-" + uid);
        body.put("nama", "Detail Test");
        body.put("email", email);
        body.put("password", "Pass1234!");
        body.put("role", "BURUH");
        given().contentType(ContentType.JSON).body(body)
            .when().post("/api/auth/register").then().statusCode(201);

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

        given()
            .header("Authorization", "Bearer " + adminToken)
        .when()
            .get("/api/admin/users/" + buruhId)
        .then()
            .statusCode(200)
            .body("status", equalTo("success"))
            .body("data.id", equalTo(buruhId))
            .body("data.email", equalTo(email))
            .body("data.role", equalTo("BURUH"))
            .body("data.walletBalance", notNullValue())
            .body("data.mandorCertificationNumber", nullValue());
    }

    @Test
    void getUserById_mandorUser_returnsCertificationNumber() {
        String uid = UUID.randomUUID().toString();
        String adminToken = loginAdmin();
        String cert = "CERT-DETAIL-" + uid;

        Map<String, Object> body = new HashMap<>();
        body.put("username", "mandor-detail-" + uid);
        body.put("nama", "Mandor Detail");
        body.put("email", "mandor-detail-" + uid + "@test.com");
        body.put("password", "Pass1234!");
        body.put("role", "MANDOR");
        body.put("mandorCertificationNumber", cert);
        given().contentType(ContentType.JSON).body(body)
            .when().post("/api/auth/register").then().statusCode(201);

        String mandorToken = given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", "mandor-detail-" + uid + "@test.com", "password", "Pass1234!"))
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(200)
            .extract().path("token");

        String mandorId = given()
            .header("Authorization", "Bearer " + mandorToken)
        .when()
            .get("/api/auth/me")
        .then()
            .statusCode(200)
            .extract().path("id");

        given()
            .header("Authorization", "Bearer " + adminToken)
        .when()
            .get("/api/admin/users/" + mandorId)
        .then()
            .statusCode(200)
            .body("data.mandorCertificationNumber", equalTo(cert))
            .body("data.mandorId", nullValue());
    }

    @Test
    void getUserById_unknownId_returns404() {
        String adminToken = loginAdmin();

        given()
            .header("Authorization", "Bearer " + adminToken)
        .when()
            .get("/api/admin/users/" + UUID.randomUUID())
        .then()
            .statusCode(404);
    }

    @Test
    void getUserById_asBuruh_returns403() {
        String uid = UUID.randomUUID().toString();

        Map<String, Object> body = new HashMap<>();
        body.put("username", "buruh-sec-" + uid);
        body.put("nama", "Buruh Sec");
        body.put("email", "buruh-sec-" + uid + "@test.com");
        body.put("password", "Pass1234!");
        body.put("role", "BURUH");
        String buruhToken = given().contentType(ContentType.JSON).body(body)
            .when().post("/api/auth/register").then().statusCode(201)
            .extract().path("token");

        given()
            .header("Authorization", "Bearer " + buruhToken)
        .when()
            .get("/api/admin/users/" + UUID.randomUUID())
        .then()
            .statusCode(403);
    }
}
