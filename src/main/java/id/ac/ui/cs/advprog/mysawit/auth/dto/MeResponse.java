package id.ac.ui.cs.advprog.mysawit.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class MeResponse {
    private String id;
    private String email;
    private String username;
    private String authProvider;
}