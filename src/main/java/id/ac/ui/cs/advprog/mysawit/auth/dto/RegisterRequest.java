package id.ac.ui.cs.advprog.mysawit.auth.dto;
import id.ac.ui.cs.advprog.mysawit.auth.entity.Role;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(max = 50, message = "Username must be at most 50 characters")
    @Pattern(regexp = "^\\S+$", message = "Username must not contain whitespace")
    private String username;

    @NotBlank(message = "Nama is required")
    @Size(max = 100, message = "Nama must be at most 100 characters")
    private String nama;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$",
        message = "Password must be at least 8 characters and contain at least 1 uppercase letter,"
            + " 1 digit, and 1 special character"
    )
    private String password;

    @NotNull(message = "Role is required")
    private Role role;

    private String mandorCertificationNumber;
}
