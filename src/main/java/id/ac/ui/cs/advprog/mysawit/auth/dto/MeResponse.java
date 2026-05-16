package id.ac.ui.cs.advprog.mysawit.auth.dto;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MeResponse {
    private String id;
    private String email;
    private String username;
    private String nama;
    private String role;
    private String mandorCertificationNumber;
    private String mandorId;
    private String authProvider;
    private LocalDateTime createdAt;
}
