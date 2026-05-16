package id.ac.ui.cs.advprog.mysawit.auth.dto;
import lombok.*;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MandorAssignmentsResponse {
    private UUID mandorId;
    private String mandorNama;
    private List<AssignmentResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
