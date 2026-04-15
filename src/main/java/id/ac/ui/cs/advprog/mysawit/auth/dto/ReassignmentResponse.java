package id.ac.ui.cs.advprog.mysawit.auth.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReassignmentResponse {
    private UUID assignmentId;
    private UUID buruhId;
    private String buruhName;
    private UUID oldMandorId;
    private String oldMandorName;
    private UUID newMandorId;
    private String newMandorName;
    private LocalDateTime reassignedAt;
}
