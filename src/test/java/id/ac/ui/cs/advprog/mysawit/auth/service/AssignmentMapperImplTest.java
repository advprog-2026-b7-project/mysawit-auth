package id.ac.ui.cs.advprog.mysawit.auth.service;

import id.ac.ui.cs.advprog.mysawit.auth.dto.AssignmentResponse;
import id.ac.ui.cs.advprog.mysawit.auth.entity.Assignment;
import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser;
import id.ac.ui.cs.advprog.mysawit.auth.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AssignmentMapperImplTest {

    private AssignmentMapperImpl mapper;

    private AuthUser buruh;
    private AuthUser mandor;
    private Assignment assignment;
    private final UUID assignmentId = UUID.randomUUID();
    private final UUID buruhId = UUID.randomUUID();
    private final UUID mandorId = UUID.randomUUID();
    private final LocalDateTime createdAt = LocalDateTime.now();
    private final LocalDateTime reassignedAt = LocalDateTime.now().plusDays(1);

    @BeforeEach
    void setUp() {
        mapper = new AssignmentMapperImpl();

        buruh = AuthUser.builder()
                .id(buruhId)
                .username("buruh_user")
                .nama("Budi Santoso")
                .role(Role.BURUH)
                .email("buruh@test.com")
                .build();

        mandor = AuthUser.builder()
                .id(mandorId)
                .username("mandor_user")
                .nama("Mandor Jaya")
                .role(Role.MANDOR)
                .email("mandor@test.com")
                .build();

        assignment = Assignment.builder()
                .id(assignmentId)
                .buruh(buruh)
                .mandor(mandor)
                .build();
    }

    @Test
    void toResponse_allFieldsMapped() {
        AssignmentResponse response = mapper.toResponse(assignment);

        assertThat(response.getId()).isEqualTo(assignmentId);
        assertThat(response.getBuruhId()).isEqualTo(buruhId);
        assertThat(response.getMandorId()).isEqualTo(mandorId);
    }

    @Test
    void toResponse_namaUsedWhenPresent() {
        AssignmentResponse response = mapper.toResponse(assignment);

        assertThat(response.getBuruhNama()).isEqualTo("Budi Santoso");
        assertThat(response.getMandorNama()).isEqualTo("Mandor Jaya");
    }

    @Test
    void toResponse_usernameWhenNamaIsNull_buruh() {
        buruh.setNama(null);
        Assignment a = Assignment.builder()
                .id(assignmentId)
                .buruh(buruh)
                .mandor(mandor)
                .build();

        AssignmentResponse response = mapper.toResponse(a);

        assertThat(response.getBuruhNama()).isEqualTo("buruh_user");
    }

    @Test
    void toResponse_usernameWhenNamaIsNull_mandor() {
        mandor.setNama(null);
        Assignment a = Assignment.builder()
                .id(assignmentId)
                .buruh(buruh)
                .mandor(mandor)
                .build();

        AssignmentResponse response = mapper.toResponse(a);

        assertThat(response.getMandorNama()).isEqualTo("mandor_user");
    }
}
