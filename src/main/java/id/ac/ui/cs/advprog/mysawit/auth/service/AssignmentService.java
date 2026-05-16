package id.ac.ui.cs.advprog.mysawit.auth.service;
import id.ac.ui.cs.advprog.mysawit.auth.dto.AssignmentResponse;
import id.ac.ui.cs.advprog.mysawit.auth.dto.CreateAssignmentRequest;
import id.ac.ui.cs.advprog.mysawit.auth.dto.MandorAssignmentsResponse;
import id.ac.ui.cs.advprog.mysawit.auth.dto.ReassignmentRequest;
import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface AssignmentService {
    AssignmentResponse createAssignment(CreateAssignmentRequest request, String adminId);
    Page<AssignmentResponse> getAllAssignments(AuthUser caller, Pageable pageable);
    AssignmentResponse getAssignmentById(UUID id, AuthUser caller);
    AssignmentResponse getAssignmentByBuruhId(UUID buruhId, AuthUser caller);
    MandorAssignmentsResponse getAssignmentsByMandorId(
            UUID mandorId, AuthUser caller, Pageable pageable);
    String deleteAssignment(UUID id, AuthUser caller);
    AssignmentResponse reassignBuruh(
            UUID assignmentId, ReassignmentRequest request, String adminId);
}
