package id.ac.ui.cs.advprog.mysawit.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.mysawit.auth.dto.AssignmentResponse;
import id.ac.ui.cs.advprog.mysawit.auth.dto.CreateAssignmentRequest;
import id.ac.ui.cs.advprog.mysawit.auth.dto.MandorAssignmentsResponse;
import id.ac.ui.cs.advprog.mysawit.auth.dto.ReassignmentRequest;
import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser;
import id.ac.ui.cs.advprog.mysawit.auth.entity.Role;
import id.ac.ui.cs.advprog.mysawit.auth.exception.AssignmentForbiddenException;
import id.ac.ui.cs.advprog.mysawit.auth.exception.AssignmentNotFoundException;
import id.ac.ui.cs.advprog.mysawit.auth.exception.DuplicateAssignmentException;
import id.ac.ui.cs.advprog.mysawit.auth.exception.RoleMismatchException;
import id.ac.ui.cs.advprog.mysawit.auth.service.AssignmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:mysawit_auth;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AssignmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AssignmentService assignmentService;

    @Test
    void createAssignment_asAdmin_returns201() throws Exception {
        UUID buruhId = UUID.randomUUID();
        UUID mandorId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();

        AssignmentResponse resp = AssignmentResponse.builder()
                .id(assignmentId)
                .buruhId(buruhId)
                .mandorId(mandorId)
                .assignedAt(LocalDateTime.now())
                .build();

        when(assignmentService.createAssignment(any(CreateAssignmentRequest.class), any()))
                .thenReturn(resp);

        CreateAssignmentRequest req = new CreateAssignmentRequest(buruhId, mandorId);

        mockMvc.perform(post("/api/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(authentication(buildAuth(UUID.randomUUID(), Role.ADMIN))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(assignmentId.toString()));
    }

    @Test
    void createAssignment_asBuruh_returns403() throws Exception {
        UUID buruhId = UUID.randomUUID();
        UUID mandorId = UUID.randomUUID();
        CreateAssignmentRequest req = new CreateAssignmentRequest(buruhId, mandorId);

        mockMvc.perform(post("/api/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(authentication(buildAuth(UUID.randomUUID(), Role.BURUH))))
                .andExpect(status().isForbidden());
    }

    @Test
    void createAssignment_withoutAuth_returns401() throws Exception {
        UUID buruhId = UUID.randomUUID();
        UUID mandorId = UUID.randomUUID();
        CreateAssignmentRequest req = new CreateAssignmentRequest(buruhId, mandorId);

        mockMvc.perform(post("/api/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createAssignment_duplicateBuruh_returns409() throws Exception {
        UUID buruhId = UUID.randomUUID();
        UUID mandorId = UUID.randomUUID();
        CreateAssignmentRequest req = new CreateAssignmentRequest(buruhId, mandorId);

        when(assignmentService.createAssignment(any(), any()))
                .thenThrow(new DuplicateAssignmentException("Buruh already assigned"));

        mockMvc.perform(post("/api/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(authentication(buildAuth(UUID.randomUUID(), Role.ADMIN))))
                .andExpect(status().isConflict());
    }

    @Test
    void getAllAssignments_authenticated_returns200() throws Exception {
        Page<AssignmentResponse> page = new PageImpl<>(
                List.of(), PageRequest.of(0, 20), 0);

        when(assignmentService.getAllAssignments(any(AuthUser.class), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/assignments")
                        .with(authentication(buildAuth(UUID.randomUUID(), Role.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void getAllAssignments_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/assignments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAssignmentById_found_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        AssignmentResponse resp = AssignmentResponse.builder()
                .id(id)
                .buruhId(UUID.randomUUID())
                .mandorId(UUID.randomUUID())
                .assignedAt(LocalDateTime.now())
                .build();

        when(assignmentService.getAssignmentById(eq(id), any(AuthUser.class))).thenReturn(resp);

        mockMvc.perform(get("/api/assignments/" + id)
                        .with(authentication(buildAuth(UUID.randomUUID(), Role.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id.toString()));
    }

    @Test
    void getAssignmentById_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();

        when(assignmentService.getAssignmentById(eq(id), any(AuthUser.class)))
                .thenThrow(new AssignmentNotFoundException("Assignment not found"));

        mockMvc.perform(get("/api/assignments/" + id)
                        .with(authentication(buildAuth(UUID.randomUUID(), Role.ADMIN))))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAssignmentById_accessDenied_returns403() throws Exception {
        UUID id = UUID.randomUUID();

        when(assignmentService.getAssignmentById(eq(id), any(AuthUser.class)))
                .thenThrow(new AssignmentForbiddenException("Access denied"));

        mockMvc.perform(get("/api/assignments/" + id)
                        .with(authentication(buildAuth(UUID.randomUUID(), Role.BURUH))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAssignmentByBuruhId_found_returns200() throws Exception {
        UUID buruhId = UUID.randomUUID();
        AssignmentResponse resp = AssignmentResponse.builder()
                .id(UUID.randomUUID())
                .buruhId(buruhId)
                .mandorId(UUID.randomUUID())
                .build();

        when(assignmentService.getAssignmentByBuruhId(eq(buruhId), any(AuthUser.class)))
                .thenReturn(resp);

        mockMvc.perform(get("/api/assignments/buruh/" + buruhId)
                        .with(authentication(buildAuth(UUID.randomUUID(), Role.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.buruhId").value(buruhId.toString()));
    }

    @Test
    void getAssignmentByBuruhId_forbidden_returns403() throws Exception {
        UUID buruhId = UUID.randomUUID();

        when(assignmentService.getAssignmentByBuruhId(eq(buruhId), any(AuthUser.class)))
                .thenThrow(new AssignmentForbiddenException("Forbidden"));

        mockMvc.perform(get("/api/assignments/buruh/" + buruhId)
                        .with(authentication(buildAuth(UUID.randomUUID(), Role.BURUH))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMandorAssignments_found_returns200() throws Exception {
        UUID mandorId = UUID.randomUUID();
        MandorAssignmentsResponse resp = MandorAssignmentsResponse.builder()
                .mandorId(mandorId)
                .mandorNama("Mandor X")
                .content(List.of())
                .totalElements(0)
                .build();

        when(assignmentService.getAssignmentsByMandorId(eq(mandorId), any(AuthUser.class), any()))
                .thenReturn(resp);

        mockMvc.perform(get("/api/assignments/mandor/" + mandorId)
                        .with(authentication(buildAuth(UUID.randomUUID(), Role.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mandorId").value(mandorId.toString()));
    }

    @Test
    void getMandorAssignments_forbidden_returns403() throws Exception {
        UUID mandorId = UUID.randomUUID();

        when(assignmentService.getAssignmentsByMandorId(eq(mandorId), any(AuthUser.class), any()))
                .thenThrow(new AssignmentForbiddenException("Forbidden"));

        mockMvc.perform(get("/api/assignments/mandor/" + mandorId)
                        .with(authentication(buildAuth(UUID.randomUUID(), Role.MANDOR))))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteAssignment_withNewMandorId_returns200WithReassignedData() throws Exception {
        UUID id = UUID.randomUUID();
        UUID newMandorId = UUID.randomUUID();

        AssignmentResponse resp = AssignmentResponse.builder()
                .id(id)
                .mandorId(newMandorId)
                .assignedAt(LocalDateTime.now())
                .reassignedAt(LocalDateTime.now())
                .build();

        when(assignmentService.reassignOnDelete(eq(id), any(AuthUser.class), eq(newMandorId)))
                .thenReturn(resp);

        mockMvc.perform(delete("/api/assignments/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newMandorId\":\"" + newMandorId + "\"}")
                        .with(authentication(buildAuth(UUID.randomUUID(), Role.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.mandorId").value(newMandorId.toString()));
    }

    @Test
    void deleteAssignment_withNewMandorId_sameMandor_returns400() throws Exception {
        UUID id = UUID.randomUUID();
        UUID sameMandorId = UUID.randomUUID();

        when(assignmentService.reassignOnDelete(eq(id), any(AuthUser.class), eq(sameMandorId)))
                .thenThrow(new RoleMismatchException("SAME_MANDOR"));

        mockMvc.perform(delete("/api/assignments/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newMandorId\":\"" + sameMandorId + "\"}")
                        .with(authentication(buildAuth(UUID.randomUUID(), Role.ADMIN))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteAssignment_withNewMandorId_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        UUID newMandorId = UUID.randomUUID();

        when(assignmentService.reassignOnDelete(eq(id), any(AuthUser.class), eq(newMandorId)))
                .thenThrow(new AssignmentNotFoundException("Assignment not found"));

        mockMvc.perform(delete("/api/assignments/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newMandorId\":\"" + newMandorId + "\"}")
                        .with(authentication(buildAuth(UUID.randomUUID(), Role.ADMIN))))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteAssignment_withNullNewMandorId_fallsBackToPlainDelete() throws Exception {
        UUID id = UUID.randomUUID();

        when(assignmentService.deleteAssignment(eq(id), any(AuthUser.class)))
                .thenReturn("Assignment deleted");

        mockMvc.perform(delete("/api/assignments/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newMandorId\":null}")
                        .with(authentication(buildAuth(UUID.randomUUID(), Role.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Assignment deleted"));
    }

    @Test
    void deleteAssignment_asAdmin_returns200() throws Exception {
        UUID id = UUID.randomUUID();

        when(assignmentService.deleteAssignment(eq(id), any(AuthUser.class)))
                .thenReturn("Assignment deleted");

        mockMvc.perform(delete("/api/assignments/" + id)
                        .with(authentication(buildAuth(UUID.randomUUID(), Role.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Assignment deleted"));
    }

    @Test
    void deleteAssignment_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();

        when(assignmentService.deleteAssignment(eq(id), any(AuthUser.class)))
                .thenThrow(new AssignmentNotFoundException("Not found"));

        mockMvc.perform(delete("/api/assignments/" + id)
                        .with(authentication(buildAuth(UUID.randomUUID(), Role.ADMIN))))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteAssignment_accessDenied_returns403() throws Exception {
        UUID id = UUID.randomUUID();

        when(assignmentService.deleteAssignment(eq(id), any(AuthUser.class)))
                .thenThrow(new AssignmentForbiddenException("Forbidden"));

        mockMvc.perform(delete("/api/assignments/" + id)
                        .with(authentication(buildAuth(UUID.randomUUID(), Role.BURUH))))
                .andExpect(status().isForbidden());
    }

    @Test
    void reassignBuruh_asAdmin_returns200() throws Exception {
        UUID assignmentId = UUID.randomUUID();
        UUID newMandorId = UUID.randomUUID();
        ReassignmentRequest req = new ReassignmentRequest(newMandorId);

        AssignmentResponse resp = AssignmentResponse.builder()
                .id(assignmentId)
                .mandorId(newMandorId)
                .assignedAt(LocalDateTime.now())
                .reassignedAt(LocalDateTime.now())
                .build();

        when(assignmentService.reassignBuruh(
                eq(assignmentId),
                any(ReassignmentRequest.class),
                any()))
                .thenReturn(resp);

        mockMvc.perform(post("/api/assignments/" + assignmentId + "/reassign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(authentication(buildAuth(UUID.randomUUID(), Role.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void reassignBuruh_asBuruh_returns403() throws Exception {
        UUID assignmentId = UUID.randomUUID();
        ReassignmentRequest req = new ReassignmentRequest(UUID.randomUUID());

        mockMvc.perform(post("/api/assignments/" + assignmentId + "/reassign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(authentication(buildAuth(UUID.randomUUID(), Role.BURUH))))
                .andExpect(status().isForbidden());
    }

    @Test
    void reassignBuruh_sameMandor_returns400() throws Exception {
        UUID assignmentId = UUID.randomUUID();
        UUID mandorId = UUID.randomUUID();
        ReassignmentRequest req = new ReassignmentRequest(mandorId);

        when(assignmentService.reassignBuruh(
                eq(assignmentId),
                any(ReassignmentRequest.class),
                any()))
                .thenThrow(new RoleMismatchException("Already assigned to this mandor"));

        mockMvc.perform(post("/api/assignments/" + assignmentId + "/reassign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(authentication(buildAuth(UUID.randomUUID(), Role.ADMIN))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reassignBuruh_notFound_returns404() throws Exception {
        UUID assignmentId = UUID.randomUUID();
        ReassignmentRequest req = new ReassignmentRequest(UUID.randomUUID());

        when(assignmentService.reassignBuruh(
                eq(assignmentId),
                any(ReassignmentRequest.class),
                any()))
                .thenThrow(new AssignmentNotFoundException("Assignment not found"));

        mockMvc.perform(post("/api/assignments/" + assignmentId + "/reassign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(authentication(buildAuth(UUID.randomUUID(), Role.ADMIN))))
                .andExpect(status().isNotFound());
    }

    private Authentication buildAuth(UUID userId, Role role) {
        AuthUser user = AuthUser.builder()
                .id(userId)
                .username("user-" + userId)
                .email("user@test.com")
                .role(role)
                .build();
        return new UsernamePasswordAuthenticationToken(
                user,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );
    }
}
