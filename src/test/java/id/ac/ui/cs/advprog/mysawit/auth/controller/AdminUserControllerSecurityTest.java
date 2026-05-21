package id.ac.ui.cs.advprog.mysawit.auth.controller;

import id.ac.ui.cs.advprog.mysawit.auth.dto.AdminUserDetailResponse;
import id.ac.ui.cs.advprog.mysawit.auth.dto.AdminUserResponse;
import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser;
import id.ac.ui.cs.advprog.mysawit.auth.entity.Role;
import id.ac.ui.cs.advprog.mysawit.auth.service.AdminUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class AdminUserControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminUserService adminUserService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUsers_shouldAllowAdminAndPassFilters() throws Exception {
        AdminUserResponse user = AdminUserResponse.builder()
                .id(UUID.randomUUID())
                .username("Alice")
                .email("alice@example.com")
                .role(Role.MANDOR)
                .build();

        Pageable defaultPageable = PageRequest.of(0, 20);
        Page<AdminUserResponse> page = new PageImpl<>(List.of(user), defaultPageable, 1);

        when(adminUserService.getUsers(
                eq("ali"), eq("example"), eq(Role.MANDOR), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(
                        get("/api/admin/users")
                                .param("name", "ali")
                                .param("email", "example")
                                .param("role", "MANDOR")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.content[0].email").value("alice@example.com"))
                .andExpect(jsonPath("$.data.content[0].username").value("Alice"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1));

        verify(adminUserService).getUsers(
                eq("ali"), eq("example"), eq(Role.MANDOR), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "MANDOR")
    void getUsers_shouldRejectNonAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminUserService);
    }

    @Test
    @WithAnonymousUser
    void getUsers_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(adminUserService);
    }

    @Test
    void getUserById_asAdmin_returns200WithDetailResponse() throws Exception {
        UUID userId = UUID.randomUUID();
        AdminUserDetailResponse detail = AdminUserDetailResponse.builder()
                .id(userId)
                .username("alice")
                .nama("Alice")
                .email("alice@example.com")
                .role(Role.BURUH)
                .walletBalance(BigDecimal.ZERO)
                .mandorCertificationNumber(null)
                .mandorId(null)
                .kebunId(null)
                .kebunNama(null)
                .build();

        when(adminUserService.getUserById(userId)).thenReturn(detail);

        mockMvc.perform(
                        get("/api/admin/users/{id}", userId)
                                .with(authentication(adminAuthentication(UUID.randomUUID())))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(userId.toString()))
                .andExpect(jsonPath("$.data.email").value("alice@example.com"))
                .andExpect(jsonPath("$.data.role").value("BURUH"));

        verify(adminUserService).getUserById(userId);
    }

    @Test
    @WithMockUser(roles = "BURUH")
    void getUserById_asNonAdmin_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/users/{id}", UUID.randomUUID()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminUserService);
    }

    @Test
    @WithAnonymousUser
    void getUserById_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/users/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(adminUserService);
    }

    @Test
    void getUserById_notFound_returns404() throws Exception {
        UUID userId = UUID.randomUUID();

        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND"))
                .when(adminUserService).getUserById(userId);

        mockMvc.perform(
                        get("/api/admin/users/{id}", userId)
                                .with(authentication(adminAuthentication(UUID.randomUUID())))
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUser_shouldAllowAdminDeletingOtherUser() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        when(adminUserService.deleteUser(targetUserId, adminId))
                .thenReturn("User target@example.com successfully deleted.");

        mockMvc.perform(
                        delete("/api/admin/users/{id}", targetUserId)
                                .with(authentication(adminAuthentication(adminId)))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("User target@example.com successfully deleted."));

        verify(adminUserService).deleteUser(targetUserId, adminId);
    }

    @Test
    void deleteUser_shouldRejectAdminSelfDelete() throws Exception {
        UUID adminId = UUID.randomUUID();

        doThrow(new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Admin cannot delete their own account"
                )).when(adminUserService).deleteUser(adminId, adminId);

        mockMvc.perform(
                        delete("/api/admin/users/{id}", adminId)
                                .with(authentication(adminAuthentication(adminId)))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANDOR")
    void deleteUser_shouldRejectNonAdmin() throws Exception {
        mockMvc.perform(delete("/api/admin/users/{id}", UUID.randomUUID()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminUserService);
    }

    private Authentication adminAuthentication(UUID adminId) {
        AuthUser admin = AuthUser.builder()
                .id(adminId)
                .username("admin")
                .email("admin@example.com")
                .role(Role.ADMIN)
                .build();

        return new UsernamePasswordAuthenticationToken(
                admin,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }
}
