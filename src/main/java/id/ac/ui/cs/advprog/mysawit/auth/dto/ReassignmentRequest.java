package id.ac.ui.cs.advprog.mysawit.auth.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReassignmentRequest {
    private UUID newMandorId;
}
