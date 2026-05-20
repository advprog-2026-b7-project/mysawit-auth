package id.ac.ui.cs.advprog.mysawit.auth.dto;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentResponse {
    private UUID id;
    private UUID buruhId;
    private String buruhNama;
    private UUID mandorId;
    private String mandorNama;
    private String plantationId;
    private LocalDateTime assignedAt;
    private LocalDateTime reassignedAt;
}
