package id.ac.ui.cs.advprog.mysawit.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateMeRequest {
    private String nama;
    private String username;
    private String currentPassword;
    private String newPassword;
}
