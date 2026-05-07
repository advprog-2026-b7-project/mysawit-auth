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
    private String buruhName;
    private UUID mandorId;
    private String mandorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
