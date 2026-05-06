package id.ac.ui.cs.advprog.mysawit.auth.controller;
import id.ac.ui.cs.advprog.mysawit.auth.dto.AdminUserResponse;

import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser;
import id.ac.ui.cs.advprog.mysawit.auth.entity.Role;
import id.ac.ui.cs.advprog.mysawit.auth.service.AdminUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.doThrow;
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

        when(adminUserService.getUsers("ali", "example", Role.MANDOR))
                .thenReturn(List.of(user));

        mockMvc.perform(
                        get("/api/admin/users")
                                .param("name", "ali")
                                .param("email", "example")
                                .param("role", "MANDOR")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("alice@example.com"))
                .andExpect(jsonPath("$[0].username").value("Alice"));

        verify(adminUserService).getUsers("ali", "example", Role.MANDOR);
    }

    @Test
    @WithMockUser(roles = "MANDOR")
    void getUsers_shouldRejectNonAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminUserService);
    }

    @Test
    void deleteUser_shouldAllowAdminDeletingOtherUser() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        mockMvc.perform(
                        delete("/api/admin/users/{id}", targetUserId)
                                .with(authentication(adminAuthentication(adminId)))
                )
                .andExpect(status().isNoContent());

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
