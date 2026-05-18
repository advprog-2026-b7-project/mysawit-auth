package id.ac.ui.cs.advprog.mysawit.auth.service;

import id.ac.ui.cs.advprog.mysawit.auth.entity.Assignment;
import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser;
import id.ac.ui.cs.advprog.mysawit.auth.entity.Role;
import id.ac.ui.cs.advprog.mysawit.auth.exception.AssignmentForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class AssignmentAccessPolicyImplTest {

    private AssignmentAccessPolicyImpl policy;

    private final UUID buruhId = UUID.randomUUID();
    private final UUID mandorId = UUID.randomUUID();
    private final UUID otherId = UUID.randomUUID();

    private AuthUser buruhCaller;
    private AuthUser mandorCaller;
    private AuthUser adminCaller;
    private AuthUser supirCaller;
    private AuthUser otherBuruh;
    private AuthUser otherMandor;
    private Assignment assignment;

    @BeforeEach
    void setUp() {
        policy = new AssignmentAccessPolicyImpl();

        buruhCaller = AuthUser.builder().id(buruhId).role(Role.BURUH)
                .username("buruh").email("b@test.com").build();
        mandorCaller = AuthUser.builder().id(mandorId).role(Role.MANDOR)
                .username("mandor").email("m@test.com").build();
        adminCaller = AuthUser.builder().id(UUID.randomUUID()).role(Role.ADMIN)
                .username("admin").email("a@test.com").build();
        supirCaller = AuthUser.builder().id(UUID.randomUUID()).role(Role.SUPIR)
                .username("supir").email("s@test.com").build();
        otherBuruh = AuthUser.builder().id(otherId).role(Role.BURUH)
                .username("other_buruh").email("ob@test.com").build();
        otherMandor = AuthUser.builder().id(otherId).role(Role.MANDOR)
                .username("other_mandor").email("om@test.com").build();

        assignment = Assignment.builder()
                .id(UUID.randomUUID())
                .buruh(buruhCaller)
                .mandor(mandorCaller)
                .build();
    }

    @Test
    void checkReadAccess_adminAllowed() {
        assertThatCode(() -> policy.checkReadAccess(adminCaller, assignment))
                .doesNotThrowAnyException();
    }

    @Test
    void checkReadAccess_mandorOwnAllowed() {
        assertThatCode(() -> policy.checkReadAccess(mandorCaller, assignment))
                .doesNotThrowAnyException();
    }

    @Test
    void checkReadAccess_mandorOtherForbidden() {
        Assignment other = Assignment.builder()
                .id(UUID.randomUUID()).buruh(buruhCaller).mandor(otherMandor).build();
        assertThatThrownBy(() -> policy.checkReadAccess(mandorCaller, other))
                .isInstanceOf(AssignmentForbiddenException.class)
                .hasMessage("Access denied to this assignment");
    }

    @Test
    void checkReadAccess_buruhOwnAllowed() {
        assertThatCode(() -> policy.checkReadAccess(buruhCaller, assignment))
                .doesNotThrowAnyException();
    }

    @Test
    void checkReadAccess_buruhOtherForbidden() {
        Assignment other = Assignment.builder()
                .id(UUID.randomUUID()).buruh(otherBuruh).mandor(mandorCaller).build();
        assertThatThrownBy(() -> policy.checkReadAccess(buruhCaller, other))
                .isInstanceOf(AssignmentForbiddenException.class)
                .hasMessage("Access denied to this assignment");
    }

    @Test
    void checkReadAccess_supirAllowed() {
        assertThatCode(() -> policy.checkReadAccess(supirCaller, assignment))
                .doesNotThrowAnyException();
    }

    // --- checkDeleteAccess ---

    @Test
    void checkDeleteAccess_adminAllowed() {
        assertThatCode(() -> policy.checkDeleteAccess(adminCaller, assignment))
                .doesNotThrowAnyException();
    }

    @Test
    void checkDeleteAccess_mandorOwnAllowed() {
        assertThatCode(() -> policy.checkDeleteAccess(mandorCaller, assignment))
                .doesNotThrowAnyException();
    }

    @Test
    void checkDeleteAccess_mandorOtherForbidden() {
        Assignment other = Assignment.builder()
                .id(UUID.randomUUID()).buruh(buruhCaller).mandor(otherMandor).build();
        assertThatThrownBy(() -> policy.checkDeleteAccess(mandorCaller, other))
                .isInstanceOf(AssignmentForbiddenException.class)
                .hasMessage("Mandor may only remove assignments of their own Buruh");
    }

    @Test
    void checkDeleteAccess_buruhOwnAllowed() {
        assertThatCode(() -> policy.checkDeleteAccess(buruhCaller, assignment))
                .doesNotThrowAnyException();
    }

    @Test
    void checkDeleteAccess_buruhOtherForbidden() {
        Assignment other = Assignment.builder()
                .id(UUID.randomUUID()).buruh(otherBuruh).mandor(mandorCaller).build();
        assertThatThrownBy(() -> policy.checkDeleteAccess(buruhCaller, other))
                .isInstanceOf(AssignmentForbiddenException.class)
                .hasMessage("Buruh may only remove their own assignment");
    }

    @Test
    void checkDeleteAccess_supirAllowed() {
        assertThatCode(() -> policy.checkDeleteAccess(supirCaller, assignment))
                .doesNotThrowAnyException();
    }


    @Test
    void checkBuruhQueryAccess_buruhOwnAllowed() {
        assertThatCode(() -> policy.checkBuruhQueryAccess(buruhCaller, buruhId))
                .doesNotThrowAnyException();
    }

    @Test
    void checkBuruhQueryAccess_buruhOtherForbidden() {
        assertThatThrownBy(() -> policy.checkBuruhQueryAccess(buruhCaller, otherId))
                .isInstanceOf(AssignmentForbiddenException.class)
                .hasMessage("Buruh may only query their own assignment");
    }

    @Test
    void checkBuruhQueryAccess_adminAllowed() {
        assertThatCode(() -> policy.checkBuruhQueryAccess(adminCaller, buruhId))
                .doesNotThrowAnyException();
    }

    @Test
    void checkBuruhQueryAccess_mandorAllowed() {
        assertThatCode(() -> policy.checkBuruhQueryAccess(mandorCaller, buruhId))
                .doesNotThrowAnyException();
    }


    @Test
    void checkMandorQueryAccess_mandorOwnAllowed() {
        assertThatCode(() -> policy.checkMandorQueryAccess(mandorCaller, mandorId))
                .doesNotThrowAnyException();
    }

    @Test
    void checkMandorQueryAccess_mandorOtherForbidden() {
        assertThatThrownBy(() -> policy.checkMandorQueryAccess(mandorCaller, otherId))
                .isInstanceOf(AssignmentForbiddenException.class)
                .hasMessage("Mandor may only query their own assignments");
    }

    @Test
    void checkMandorQueryAccess_adminAllowed() {
        assertThatCode(() -> policy.checkMandorQueryAccess(adminCaller, mandorId))
                .doesNotThrowAnyException();
    }

    @Test
    void checkMandorQueryAccess_buruhAllowed() {
        assertThatCode(() -> policy.checkMandorQueryAccess(buruhCaller, mandorId))
                .doesNotThrowAnyException();
    }
}
