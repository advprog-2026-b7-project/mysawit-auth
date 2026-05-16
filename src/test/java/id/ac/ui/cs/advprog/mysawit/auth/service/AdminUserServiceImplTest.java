package id.ac.ui.cs.advprog.mysawit.auth.service;

import id.ac.ui.cs.advprog.mysawit.auth.dto.AdminUserResponse;
import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser;
import id.ac.ui.cs.advprog.mysawit.auth.entity.Role;
import id.ac.ui.cs.advprog.mysawit.auth.repository.AuthUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @Mock
    private AuthUserRepository authUserRepository;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    @Test
    void getUsers_shouldNormalizeFiltersAndMapResponse() {
        UUID mandorId = UUID.randomUUID();
        AuthUser mandor = AuthUser.builder()
                .id(mandorId)
                .username("Mandor")
                .email("mandor@example.com")
                .role(Role.MANDOR)
                .build();

        AuthUser user = AuthUser.builder()
                .id(UUID.randomUUID())
                .username("Alice")
                .email("alice@example.com")
                .role(Role.BURUH)
                .password("hidden")
                .mandorCertificationNumber("CERT-123")
                .mandor(mandor)
                .build();

        Pageable pageable = PageRequest.of(0, 20);
        Page<AuthUser> pageResult = new PageImpl<>(List.of(user), pageable, 1);

        when(authUserRepository.findUsersByFilters("Alice", null, Role.BURUH, pageable))
                .thenReturn(pageResult);

        Page<AdminUserResponse> result =
                adminUserService.getUsers("  Alice  ", " ", Role.BURUH, pageable);

        assertEquals(1, result.getTotalElements());
        AdminUserResponse first = result.getContent().getFirst();
        assertEquals(user.getId(), first.getId());
        assertEquals("Alice", first.getUsername());
        assertEquals("alice@example.com", first.getEmail());
        assertEquals(Role.BURUH, first.getRole());
        assertEquals("CERT-123", first.getMandorCertificationNumber());
        assertEquals(mandorId, first.getMandorId());
        verify(authUserRepository).findUsersByFilters("Alice", null, Role.BURUH, pageable);
    }

    @Test
    void getUsers_shouldReturnNullMandorIdWhenMandorIsAbsent() {
        AuthUser user = AuthUser.builder()
                .id(UUID.randomUUID())
                .username("Admin")
                .email("admin@example.com")
                .role(Role.ADMIN)
                .build();

        Pageable pageable = PageRequest.of(0, 20);
        Page<AuthUser> pageResult = new PageImpl<>(List.of(user), pageable, 1);

        when(authUserRepository.findUsersByFilters(null, null, null, pageable))
                .thenReturn(pageResult);

        Page<AdminUserResponse> result = adminUserService.getUsers(null, null, null, pageable);

        assertNull(result.getContent().getFirst().getMandorId());
    }

    @Test
    void deleteUser_shouldRejectAdminSelfDelete() {
        UUID adminId = UUID.randomUUID();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> adminUserService.deleteUser(adminId, adminId)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verifyNoInteractions(authUserRepository);
    }

    @Test
    void deleteUser_shouldThrowNotFoundWhenTargetDoesNotExist() {
        UUID adminId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        when(authUserRepository.findById(targetUserId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> adminUserService.deleteUser(targetUserId, adminId)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(authUserRepository, never())
                .delete(org.mockito.ArgumentMatchers.any(AuthUser.class));
    }

    @Test
    void deleteUser_shouldDeleteTargetUserWhenAllowed() {
        UUID adminId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        AuthUser targetUser = AuthUser.builder()
                .id(targetUserId)
                .username("Target")
                .email("target@example.com")
                .role(Role.BURUH)
                .build();

        when(authUserRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));

        String message = adminUserService.deleteUser(targetUserId, adminId);

        verify(authUserRepository).delete(targetUser);
        assertEquals("User target@example.com successfully deleted.", message);
    }
}
