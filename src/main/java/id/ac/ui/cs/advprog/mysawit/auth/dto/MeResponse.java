package id.ac.ui.cs.advprog.mysawit.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MeResponse {
    private String id;
    private String email;
    private String username;
    private String role;
    private String mandorCertificationNumber;
    private String mandorId;
    private String createdAt;
    private String updatedAt;
}