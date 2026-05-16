package id.ac.ui.cs.advprog.mysawit.auth.dto;
import id.ac.ui.cs.advprog.mysawit.auth.entity.Role;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoogleLoginRequest {

    @NotBlank(message = "Google ID token is required")
    private String idToken;

    /** Required when the Google email is not yet registered. */
    private Role role;

    /** Required when role=MANDOR and this is a first-time registration. */
    private String mandorCertificationNumber;
}
