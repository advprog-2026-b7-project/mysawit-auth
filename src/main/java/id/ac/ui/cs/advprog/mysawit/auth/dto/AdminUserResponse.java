package id.ac.ui.cs.advprog.mysawit.auth.dto;
import id.ac.ui.cs.advprog.mysawit.auth.entity.Role;
import lombok.Builder;
import lombok.Value;
import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class AdminUserResponse {
    UUID id;
    String username;
    String nama;
    String email;
    Role role;
    String mandorCertificationNumber;
    UUID mandorId;
    LocalDateTime createdAt;
}
