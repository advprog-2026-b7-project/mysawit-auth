package id.ac.ui.cs.advprog.mysawit.auth.event;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class BuruhReassignedEvent {
    private final UUID assignmentId;
    private final UUID buruhId;
    private final UUID oldMandorId;
    private final UUID newMandorId;
    private final LocalDateTime reassignedAt;
}
