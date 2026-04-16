package id.ac.ui.cs.advprog.mysawit.auth.service;

import id.ac.ui.cs.advprog.mysawit.auth.dto.AssignmentResponse;
import id.ac.ui.cs.advprog.mysawit.auth.dto.CreateAssignmentRequest;
import id.ac.ui.cs.advprog.mysawit.auth.dto.ReassignmentRequest;
import id.ac.ui.cs.advprog.mysawit.auth.dto.ReassignmentResponse;

import java.util.List;
import java.util.UUID;

public interface AssignmentService {
    AssignmentResponse createAssignment(CreateAssignmentRequest request, String adminId);
    AssignmentResponse getAssignmentById(UUID id);
    List<AssignmentResponse> getAllAssignments();
    List<AssignmentResponse> getAssignmentsByBuruhId(UUID buruhId);
    List<AssignmentResponse> getAssignmentsByMandorId(UUID mandorId);
    void deleteAssignment(UUID id);
    ReassignmentResponse reassignBuruh(UUID assignmentId, ReassignmentRequest request, String adminId);
}
