package id.ac.ui.cs.advprog.mysawit.auth.service;

import id.ac.ui.cs.advprog.mysawit.auth.dto.AssignmentResponse;
import id.ac.ui.cs.advprog.mysawit.auth.dto.CreateAssignmentRequest;
import id.ac.ui.cs.advprog.mysawit.auth.dto.MandorAssignmentsResponse;
import id.ac.ui.cs.advprog.mysawit.auth.dto.ReassignmentRequest;
import id.ac.ui.cs.advprog.mysawit.auth.entity.Assignment;
import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser;
import id.ac.ui.cs.advprog.mysawit.auth.entity.Role;
import id.ac.ui.cs.advprog.mysawit.auth.event.BuruhAssignedEvent;
import id.ac.ui.cs.advprog.mysawit.auth.event.BuruhReassignedEvent;
import id.ac.ui.cs.advprog.mysawit.auth.exception.AssignmentForbiddenException;
import id.ac.ui.cs.advprog.mysawit.auth.exception.AssignmentNotFoundException;
import id.ac.ui.cs.advprog.mysawit.auth.exception.DuplicateAssignmentException;
import id.ac.ui.cs.advprog.mysawit.auth.exception.RoleMismatchException;
import id.ac.ui.cs.advprog.mysawit.auth.exception.UserNotFoundException;
import id.ac.ui.cs.advprog.mysawit.auth.repository.AssignmentRepository;
import id.ac.ui.cs.advprog.mysawit.auth.repository.AuthUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceImplTest {

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private AuthUserRepository authUserRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AssignmentMapper mapper;

    @Mock
    private AssignmentAccessPolicy accessPolicy;

    @InjectMocks
    private AssignmentServiceImpl service;

    private final UUID adminId = UUID.randomUUID();
    private final UUID buruhId = UUID.randomUUID();
    private final UUID mandorId = UUID.randomUUID();
    private final UUID assignmentId = UUID.randomUUID();

    private AuthUser admin;
    private AuthUser buruh;
    private AuthUser mandor;
    private AuthUser supir;
    private Assignment assignment;
    private AssignmentResponse response;

    @BeforeEach
    void setUp() {
        admin = AuthUser.builder().id(adminId).role(Role.ADMIN)
                .username("admin").email("admin@test.com").build();
        buruh = AuthUser.builder().id(buruhId).role(Role.BURUH)
                .username("buruh").email("buruh@test.com").build();
        mandor = AuthUser.builder().id(mandorId).role(Role.MANDOR)
                .username("mandor").email("mandor@test.com").build();
        supir = AuthUser.builder().id(UUID.randomUUID()).role(Role.SUPIR)
                .username("supir").email("supir@test.com").build();

        assignment = Assignment.builder()
                .id(assignmentId).buruh(buruh).mandor(mandor).build();

        response = AssignmentResponse.builder()
                .id(assignmentId).buruhId(buruhId).mandorId(mandorId).build();
    }


    @Test
    void createAssignment_success() {
        CreateAssignmentRequest req = new CreateAssignmentRequest();
        req.setBuruhId(buruhId);
        req.setMandorId(mandorId);

        when(authUserRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(authUserRepository.findById(buruhId)).thenReturn(Optional.of(buruh));
        when(authUserRepository.findById(mandorId)).thenReturn(Optional.of(mandor));
        when(assignmentRepository.existsByBuruh(buruh)).thenReturn(false);
        when(assignmentRepository.save(any())).thenReturn(assignment);
        when(mapper.toResponse(assignment)).thenReturn(response);

        AssignmentResponse result = service.createAssignment(req, adminId.toString());

        assertThat(result).isEqualTo(response);
        verify(eventPublisher).publishEvent(any(BuruhAssignedEvent.class));
    }

    @Test
    void createAssignment_nonAdminCallerRejected() {
        when(authUserRepository.findById(buruhId)).thenReturn(Optional.of(buruh));

        assertThatThrownBy(() -> service.createAssignment(
                new CreateAssignmentRequest(), buruhId.toString()))
                .isInstanceOf(RoleMismatchException.class)
                .hasMessage("Only ADMIN can create assignments");
    }

    @Test
    void createAssignment_buruhNotBuruhRole() {
        AuthUser notBuruh = AuthUser.builder().id(buruhId).role(Role.SUPIR)
                .username("x").email("x@test.com").build();
        CreateAssignmentRequest req = new CreateAssignmentRequest();
        req.setBuruhId(buruhId);
        req.setMandorId(mandorId);

        when(authUserRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(authUserRepository.findById(buruhId)).thenReturn(Optional.of(notBuruh));

        assertThatThrownBy(() -> service.createAssignment(req, adminId.toString()))
                .isInstanceOf(RoleMismatchException.class)
                .hasMessage("Target user must have the BURUH role");
    }

    @Test
    void createAssignment_mandorNotMandorRole() {
        AuthUser notMandor = AuthUser.builder().id(mandorId).role(Role.SUPIR)
                .username("y").email("y@test.com").build();
        CreateAssignmentRequest req = new CreateAssignmentRequest();
        req.setBuruhId(buruhId);
        req.setMandorId(mandorId);

        when(authUserRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(authUserRepository.findById(buruhId)).thenReturn(Optional.of(buruh));
        when(authUserRepository.findById(mandorId)).thenReturn(Optional.of(notMandor));

        assertThatThrownBy(() -> service.createAssignment(req, adminId.toString()))
                .isInstanceOf(RoleMismatchException.class)
                .hasMessage("Assignee must have the MANDOR role");
    }

    @Test
    void createAssignment_duplicateAssignmentRejected() {
        CreateAssignmentRequest req = new CreateAssignmentRequest();
        req.setBuruhId(buruhId);
        req.setMandorId(mandorId);

        when(authUserRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(authUserRepository.findById(buruhId)).thenReturn(Optional.of(buruh));
        when(authUserRepository.findById(mandorId)).thenReturn(Optional.of(mandor));
        when(assignmentRepository.existsByBuruh(buruh)).thenReturn(true);

        assertThatThrownBy(() -> service.createAssignment(req, adminId.toString()))
                .isInstanceOf(DuplicateAssignmentException.class);
    }


    @Test
    void getAllAssignments_adminSeesAll() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Assignment> page = new PageImpl<>(List.of(assignment));
        when(assignmentRepository.findAll(pageable)).thenReturn(page);
        when(mapper.toResponse(assignment)).thenReturn(response);

        Page<AssignmentResponse> result = service.getAllAssignments(admin, pageable);

        assertThat(result.getContent()).containsExactly(response);
    }

    @Test
    void getAllAssignments_mandorSeesOwn() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Assignment> page = new PageImpl<>(List.of(assignment));
        when(authUserRepository.findById(mandorId)).thenReturn(Optional.of(mandor));
        when(assignmentRepository.findByMandor(mandor, pageable)).thenReturn(page);
        when(mapper.toResponse(assignment)).thenReturn(response);

        Page<AssignmentResponse> result = service.getAllAssignments(mandor, pageable);

        assertThat(result.getContent()).containsExactly(response);
    }

    @Test
    void getAllAssignments_buruhSeesOwn() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Assignment> page = new PageImpl<>(List.of(assignment));
        when(authUserRepository.findById(buruhId)).thenReturn(Optional.of(buruh));
        when(assignmentRepository.findByBuruh(buruh, pageable)).thenReturn(page);
        when(mapper.toResponse(assignment)).thenReturn(response);

        Page<AssignmentResponse> result = service.getAllAssignments(buruh, pageable);

        assertThat(result.getContent()).containsExactly(response);
    }

    @Test
    void getAllAssignments_supirGetsAll() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Assignment> page = new PageImpl<>(List.of(assignment));
        when(assignmentRepository.findAll(pageable)).thenReturn(page);
        when(mapper.toResponse(assignment)).thenReturn(response);

        Page<AssignmentResponse> result = service.getAllAssignments(supir, pageable);

        assertThat(result.getContent()).containsExactly(response);
    }


    @Test
    void getAssignmentById_successAdminCaller() {
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
        doNothing().when(accessPolicy).checkReadAccess(admin, assignment);
        when(mapper.toResponse(assignment)).thenReturn(response);

        AssignmentResponse result = service.getAssignmentById(assignmentId, admin);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void getAssignmentById_buruhAccessingOwn() {
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
        doNothing().when(accessPolicy).checkReadAccess(buruh, assignment);
        when(mapper.toResponse(assignment)).thenReturn(response);

        AssignmentResponse result = service.getAssignmentById(assignmentId, buruh);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void getAssignmentById_buruhAccessingOthersForbidden() {
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
        doThrow(new AssignmentForbiddenException("Access denied to this assignment"))
                .when(accessPolicy).checkReadAccess(buruh, assignment);

        assertThatThrownBy(() -> service.getAssignmentById(assignmentId, buruh))
                .isInstanceOf(AssignmentForbiddenException.class)
                .hasMessage("Access denied to this assignment");
    }


    @Test
    void getAssignmentByBuruhId_success() {
        doNothing().when(accessPolicy).checkBuruhQueryAccess(admin, buruhId);
        when(authUserRepository.findById(buruhId)).thenReturn(Optional.of(buruh));
        when(assignmentRepository.findFirstByBuruh(buruh)).thenReturn(Optional.of(assignment));
        when(mapper.toResponse(assignment)).thenReturn(response);

        AssignmentResponse result = service.getAssignmentByBuruhId(buruhId, admin);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void getAssignmentByBuruhId_buruhAccessingOtherForbidden() {
        UUID otherId = UUID.randomUUID();
        doThrow(new AssignmentForbiddenException("Buruh may only query their own assignment"))
                .when(accessPolicy).checkBuruhQueryAccess(buruh, otherId);

        assertThatThrownBy(() -> service.getAssignmentByBuruhId(otherId, buruh))
                .isInstanceOf(AssignmentForbiddenException.class)
                .hasMessage("Buruh may only query their own assignment");
    }


    @Test
    void getAssignmentsByMandorId_adminAccessingAnyMandor() {
        Pageable pageable = PageRequest.of(0, 10);
        doNothing().when(accessPolicy).checkMandorQueryAccess(admin, mandorId);
        when(authUserRepository.findById(mandorId)).thenReturn(Optional.of(mandor));
        Page<Assignment> page = new PageImpl<>(List.of(assignment));
        when(assignmentRepository.findByMandor(mandor, pageable)).thenReturn(page);
        when(mapper.toResponse(assignment)).thenReturn(response);

        MandorAssignmentsResponse result =
                service.getAssignmentsByMandorId(mandorId, admin, pageable);

        assertThat(result.getMandorId()).isEqualTo(mandorId);
        assertThat(result.getContent()).containsExactly(response);
    }

    @Test
    void getAssignmentsByMandorId_mandorAccessingOwn() {
        Pageable pageable = PageRequest.of(0, 10);
        doNothing().when(accessPolicy).checkMandorQueryAccess(mandor, mandorId);
        when(authUserRepository.findById(mandorId)).thenReturn(Optional.of(mandor));
        Page<Assignment> page = new PageImpl<>(List.of(assignment));
        when(assignmentRepository.findByMandor(mandor, pageable)).thenReturn(page);
        when(mapper.toResponse(assignment)).thenReturn(response);

        MandorAssignmentsResponse result =
                service.getAssignmentsByMandorId(mandorId, mandor, pageable);

        assertThat(result.getMandorId()).isEqualTo(mandorId);
    }

    @Test
    void getAssignmentsByMandorId_mandorAccessingOtherForbidden() {
        UUID otherId = UUID.randomUUID();
        doThrow(new AssignmentForbiddenException("Mandor may only query their own assignments"))
                .when(accessPolicy).checkMandorQueryAccess(mandor, otherId);

        assertThatThrownBy(() ->
                service.getAssignmentsByMandorId(otherId, mandor, PageRequest.of(0, 10)))
                .isInstanceOf(AssignmentForbiddenException.class)
                .hasMessage("Mandor may only query their own assignments");
    }


    @Test
    void deleteAssignment_success() {
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
        doNothing().when(accessPolicy).checkDeleteAccess(admin, assignment);

        String result = service.deleteAssignment(assignmentId, admin);

        assertThat(result).contains(assignmentId.toString());
        verify(assignmentRepository).delete(assignment);
    }

    @Test
    void deleteAssignment_notFound() {
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteAssignment(assignmentId, admin))
                .isInstanceOf(AssignmentNotFoundException.class)
                .hasMessage("Assignment not found");
    }

    @Test
    void deleteAssignment_accessDenied() {
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
        doThrow(new AssignmentForbiddenException("Buruh may only remove their own assignment"))
                .when(accessPolicy).checkDeleteAccess(buruh, assignment);

        assertThatThrownBy(() -> service.deleteAssignment(assignmentId, buruh))
                .isInstanceOf(AssignmentForbiddenException.class);
    }


    @Test
    void reassignOnDelete_success_updatesAssignmentAndPublishesEvent() {
        UUID newMandorId = UUID.randomUUID();
        AuthUser newMandor = AuthUser.builder().id(newMandorId).role(Role.MANDOR)
                .username("newmandor").email("nm@test.com").build();

        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
        doNothing().when(accessPolicy).checkDeleteAccess(admin, assignment);
        when(authUserRepository.findById(newMandorId)).thenReturn(Optional.of(newMandor));
        when(assignmentRepository.save(assignment)).thenReturn(assignment);
        when(mapper.toResponse(assignment)).thenReturn(response);

        AssignmentResponse result = service.reassignOnDelete(assignmentId, admin, newMandorId);

        assertThat(result).isEqualTo(response);
        assertThat(assignment.getMandor()).isEqualTo(newMandor);
        assertThat(assignment.getReassignedAt()).isNotNull();
        verify(eventPublisher).publishEvent(any(BuruhReassignedEvent.class));
    }

    @Test
    void reassignOnDelete_assignmentNotFound_throws() {
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reassignOnDelete(assignmentId, admin, UUID.randomUUID()))
                .isInstanceOf(AssignmentNotFoundException.class)
                .hasMessage("Assignment not found");
    }

    @Test
    void reassignOnDelete_newMandorNotFound_throwsUserNotFound() {
        UUID newMandorId = UUID.randomUUID();
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
        doNothing().when(accessPolicy).checkDeleteAccess(admin, assignment);
        when(authUserRepository.findById(newMandorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reassignOnDelete(assignmentId, admin, newMandorId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("USER_NOT_FOUND");
    }

    @Test
    void reassignOnDelete_newMandorWrongRole_throwsRoleMismatch() {
        UUID newMandorId = UUID.randomUUID();
        AuthUser notMandor = AuthUser.builder().id(newMandorId).role(Role.BURUH)
                .username("b").email("b@test.com").build();

        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
        doNothing().when(accessPolicy).checkDeleteAccess(admin, assignment);
        when(authUserRepository.findById(newMandorId)).thenReturn(Optional.of(notMandor));

        assertThatThrownBy(() -> service.reassignOnDelete(assignmentId, admin, newMandorId))
                .isInstanceOf(RoleMismatchException.class)
                .hasMessage("USER_NOT_MANDOR");
    }

    @Test
    void reassignOnDelete_sameMandor_throwsRoleMismatch() {
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
        doNothing().when(accessPolicy).checkDeleteAccess(admin, assignment);
        when(authUserRepository.findById(mandorId)).thenReturn(Optional.of(mandor));

        assertThatThrownBy(() -> service.reassignOnDelete(assignmentId, admin, mandorId))
                .isInstanceOf(RoleMismatchException.class)
                .hasMessage("SAME_MANDOR");
    }

    @Test
    void reassignOnDelete_accessDenied_throws() {
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
        doThrow(new AssignmentForbiddenException("Forbidden"))
                .when(accessPolicy).checkDeleteAccess(buruh, assignment);

        assertThatThrownBy(() -> service.reassignOnDelete(assignmentId, buruh, UUID.randomUUID()))
                .isInstanceOf(AssignmentForbiddenException.class);
    }

    @Test
    void reassignBuruh_success() {
        UUID newMandorId = UUID.randomUUID();
        AuthUser newMandor = AuthUser.builder().id(newMandorId).role(Role.MANDOR)
                .username("new_mandor").email("nm@test.com").build();
        ReassignmentRequest req = new ReassignmentRequest();
        req.setNewMandorId(newMandorId);

        when(authUserRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
        when(authUserRepository.findById(newMandorId)).thenReturn(Optional.of(newMandor));
        when(assignmentRepository.save(assignment)).thenReturn(assignment);
        when(mapper.toResponse(assignment)).thenReturn(response);

        AssignmentResponse result =
                service.reassignBuruh(assignmentId, req, adminId.toString());

        assertThat(result).isEqualTo(response);
        verify(eventPublisher).publishEvent(any(BuruhReassignedEvent.class));
    }

    @Test
    void reassignBuruh_sameMandorRejected() {
        ReassignmentRequest req = new ReassignmentRequest();
        req.setNewMandorId(mandorId);

        when(authUserRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
        when(authUserRepository.findById(mandorId)).thenReturn(Optional.of(mandor));

        assertThatThrownBy(() ->
                service.reassignBuruh(assignmentId, req, adminId.toString()))
                .isInstanceOf(RoleMismatchException.class)
                .hasMessage("New mandor is the same as the current mandor");
    }

    @Test
    void reassignBuruh_assignmentNotFound() {
        ReassignmentRequest req = new ReassignmentRequest();
        req.setNewMandorId(UUID.randomUUID());

        when(authUserRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.reassignBuruh(assignmentId, req, adminId.toString()))
                .isInstanceOf(AssignmentNotFoundException.class)
                .hasMessage("Assignment not found");
    }
}
