package id.ac.ui.cs.advprog.mysawit.auth.service;

import id.ac.ui.cs.advprog.mysawit.auth.dto.AssignmentResponse;
import id.ac.ui.cs.advprog.mysawit.auth.dto.CreateAssignmentRequest;
import id.ac.ui.cs.advprog.mysawit.auth.dto.ReassignmentRequest;
import id.ac.ui.cs.advprog.mysawit.auth.dto.ReassignmentResponse;
import id.ac.ui.cs.advprog.mysawit.auth.entity.Assignment;
import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser;
import id.ac.ui.cs.advprog.mysawit.auth.entity.Role;
import id.ac.ui.cs.advprog.mysawit.auth.repository.AssignmentRepository;
import id.ac.ui.cs.advprog.mysawit.auth.repository.AuthUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssignmentServiceImpl implements AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final AuthUserRepository authUserRepository;

    @Override
    public AssignmentResponse createAssignment(CreateAssignmentRequest request, String adminId) {
        AuthUser admin = authUserRepository.findById(UUID.fromString(adminId))
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        
        if (!admin.getRole().equals(Role.ADMIN)) {
            throw new RuntimeException("Only ADMIN can create assignments");
        }

        AuthUser buruh = authUserRepository.findById(request.getBuruhId())
                .orElseThrow(() -> new RuntimeException("Buruh not found"));
        
        if (!buruh.getRole().equals(Role.BURUH)) {
            throw new RuntimeException("User must be BURUH to be assigned");
        }

        AuthUser mandor = authUserRepository.findById(request.getMandorId())
                .orElseThrow(() -> new RuntimeException("Mandor not found"));
        
        if (!mandor.getRole().equals(Role.MANDOR)) {
            throw new RuntimeException("Assignee must be MANDOR");
        }

        Assignment assignment = Assignment.builder()
                .buruh(buruh)
                .mandor(mandor)
                .build();

        Assignment savedAssignment = assignmentRepository.save(assignment);
        return buildAssignmentResponse(savedAssignment);
    }

    @Override
    public AssignmentResponse getAssignmentById(UUID id) {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
        return buildAssignmentResponse(assignment);
    }

    @Override
    public List<AssignmentResponse> getAllAssignments() {
        return assignmentRepository.findAll()
                .stream()
                .map(this::buildAssignmentResponse)
                .toList();
    }

    @Override
    public List<AssignmentResponse> getAssignmentsByBuruhId(UUID buruhId) {
        AuthUser buruh = authUserRepository.findById(buruhId)
                .orElseThrow(() -> new RuntimeException("Buruh not found"));
        return assignmentRepository.findByBuruh(buruh)
                .stream()
                .map(this::buildAssignmentResponse)
                .toList();
    }

    @Override
    public List<AssignmentResponse> getAssignmentsByMandorId(UUID mandorId) {
        AuthUser mandor = authUserRepository.findById(mandorId)
                .orElseThrow(() -> new RuntimeException("Mandor not found"));
        return assignmentRepository.findByMandor(mandor)
                .stream()
                .map(this::buildAssignmentResponse)
                .toList();
    }

    @Override
    public void deleteAssignment(UUID id) {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
        assignmentRepository.delete(assignment);
    }

    @Override
    public ReassignmentResponse reassignBuruh(UUID assignmentId, ReassignmentRequest request, String adminId) {
        AuthUser admin = authUserRepository.findById(UUID.fromString(adminId))
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        
        if (!admin.getRole().equals(Role.ADMIN)) {
            throw new RuntimeException("Only ADMIN can reassign buruh");
        }

        Assignment currentAssignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        AuthUser buruh = currentAssignment.getBuruh();
        AuthUser oldMandor = currentAssignment.getMandor();

        AuthUser newMandor = authUserRepository.findById(request.getNewMandorId())
                .orElseThrow(() -> new RuntimeException("New mandor not found"));
        
        if (!newMandor.getRole().equals(Role.MANDOR)) {
            throw new RuntimeException("New assignee must be MANDOR");
        }

        // Check if new mandor is the same as old mandor
        if (oldMandor.getId().equals(newMandor.getId())) {
            throw new RuntimeException("New mandor is the same as current mandor");
        }

        assignmentRepository.delete(currentAssignment);

        Assignment newAssignment = Assignment.builder()
                .buruh(buruh)
                .mandor(newMandor)
                .build();

        Assignment savedAssignment = assignmentRepository.save(newAssignment);

        return buildReassignmentResponse(savedAssignment, buruh, oldMandor, newMandor);
    }

    private AssignmentResponse buildAssignmentResponse(Assignment assignment) {
        return AssignmentResponse.builder()
                .id(assignment.getId())
                .buruhId(assignment.getBuruh().getId())
                .buruhName(assignment.getBuruh().getUsername())
                .mandorId(assignment.getMandor().getId())
                .mandorName(assignment.getMandor().getUsername())
                .createdAt(assignment.getCreatedAt())
                .updatedAt(assignment.getUpdatedAt())
                .build();
    }

    private ReassignmentResponse buildReassignmentResponse(
        Assignment newAssignment,
        AuthUser buruh,
        AuthUser oldMandor,
        AuthUser newMandor) {
        return ReassignmentResponse.builder()
            .assignmentId(newAssignment.getId())
            .buruhId(buruh.getId())
            .buruhName(buruh.getUsername())
            .oldMandorId(oldMandor.getId())
            .oldMandorName(oldMandor.getUsername())
            .newMandorId(newMandor.getId())
            .newMandorName(newMandor.getUsername())
            .reassignedAt(newAssignment.getCreatedAt())
            .build();
    }
}
