package id.ac.ui.cs.advprog.mysawit.auth.event;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class BuruhAssignedEvent {
    private final UUID assignmentId;
    private final UUID buruhId;
    private final UUID mandorId;
    private final LocalDateTime assignedAt;
}
