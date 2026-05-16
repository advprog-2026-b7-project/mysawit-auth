package id.ac.ui.cs.advprog.mysawit.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String token;
    private String id;
    private String email;
    private String username;
    private String nama;
    private String role;
    private String message;
}
