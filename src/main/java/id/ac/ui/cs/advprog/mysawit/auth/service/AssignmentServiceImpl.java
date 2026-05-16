package id.ac.ui.cs.advprog.mysawit.auth.service;

import id.ac.ui.cs.advprog.mysawit.auth.dto.AssignmentResponse;
import id.ac.ui.cs.advprog.mysawit.auth.dto.CreateAssignmentRequest;
import id.ac.ui.cs.advprog.mysawit.auth.dto.MandorAssignmentsResponse;
import id.ac.ui.cs.advprog.mysawit.auth.dto.ReassignmentRequest;
import id.ac.ui.cs.advprog.mysawit.auth.entity.Assignment;
import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser;
import id.ac.ui.cs.advprog.mysawit.auth.entity.Role;
import id.ac.ui.cs.advprog.mysawit.auth.event.BuruhAssignedEvent;
import id.ac.ui.cs.advprog.mysawit.auth.event.BuruhReassignedEvent;
import id.ac.ui.cs.advprog.mysawit.auth.exception.AssignmentNotFoundException;
import id.ac.ui.cs.advprog.mysawit.auth.exception.DuplicateAssignmentException;
import id.ac.ui.cs.advprog.mysawit.auth.exception.RoleMismatchException;
import id.ac.ui.cs.advprog.mysawit.auth.exception.UserNotFoundException;
import id.ac.ui.cs.advprog.mysawit.auth.repository.AssignmentRepository;
import id.ac.ui.cs.advprog.mysawit.auth.repository.AuthUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssignmentServiceImpl implements AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final AuthUserRepository authUserRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AssignmentMapper mapper;
    private final AssignmentAccessPolicy accessPolicy;

    @Override
    @Transactional
    public AssignmentResponse createAssignment(CreateAssignmentRequest request, String adminId) {
        AuthUser admin = findUser(UUID.fromString(adminId), "Admin not found");
        requireRole(admin, Role.ADMIN, "Only ADMIN can create assignments");

        AuthUser buruh = findUser(request.getBuruhId(), "Buruh not found");
        requireRole(buruh, Role.BURUH, "Target user must have the BURUH role");

        AuthUser mandor = findUser(request.getMandorId(), "Mandor not found");
        requireRole(mandor, Role.MANDOR, "Assignee must have the MANDOR role");

        if (assignmentRepository.existsByBuruh(buruh)) {
            throw new DuplicateAssignmentException(
                    "Buruh already has an active assignment."
                    + " Use POST /api/assignments/{id}/reassign instead.");
        }

        Assignment saved = assignmentRepository.save(
                Assignment.builder().buruh(buruh).mandor(mandor).build()
        );

        eventPublisher.publishEvent(new BuruhAssignedEvent(
                saved.getId(), buruh.getId(), mandor.getId(), saved.getCreatedAt()
        ));

        return mapper.toResponse(saved);
    }

    @Override
    public Page<AssignmentResponse> getAllAssignments(AuthUser caller, Pageable pageable) {
        return switch (caller.getRole()) {
            case ADMIN -> assignmentRepository.findAll(pageable).map(mapper::toResponse);
            case MANDOR -> {
                AuthUser mandor = findUser(caller.getId(), "Mandor not found");
                yield assignmentRepository.findByMandor(mandor, pageable).map(mapper::toResponse);
            }
            case BURUH -> {
                AuthUser buruh = findUser(caller.getId(), "Buruh not found");
                yield assignmentRepository.findByBuruh(buruh, pageable).map(mapper::toResponse);
            }
            default -> assignmentRepository.findAll(pageable).map(mapper::toResponse);
        };
    }

    @Override
    public AssignmentResponse getAssignmentById(UUID id, AuthUser caller) {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new AssignmentNotFoundException("Assignment not found"));

        accessPolicy.checkReadAccess(caller, assignment);
        return mapper.toResponse(assignment);
    }

    @Override
    public AssignmentResponse getAssignmentByBuruhId(UUID buruhId, AuthUser caller) {
        accessPolicy.checkBuruhQueryAccess(caller, buruhId);

        AuthUser buruh = findUser(buruhId, "Buruh not found");
        Assignment assignment = assignmentRepository.findFirstByBuruh(buruh)
                .orElseThrow(() -> new AssignmentNotFoundException(
                        "This Buruh currently has no active assignment"));

        return mapper.toResponse(assignment);
    }

    @Override
    public MandorAssignmentsResponse getAssignmentsByMandorId(
            UUID mandorId, AuthUser caller, Pageable pageable) {
        accessPolicy.checkMandorQueryAccess(caller, mandorId);

        AuthUser mandor = findUser(mandorId, "Mandor not found");
        requireRole(mandor, Role.MANDOR, "Specified user does not have the MANDOR role");

        Page<Assignment> page = assignmentRepository.findByMandor(mandor, pageable);
        List<AssignmentResponse> content =
                page.getContent().stream().map(mapper::toResponse).toList();

        return MandorAssignmentsResponse.builder()
                .mandorId(mandor.getId())
                .mandorNama(mandor.getNama() != null ? mandor.getNama() : mandor.getUsername())
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    @Override
    @Transactional
    public String deleteAssignment(UUID id, AuthUser caller) {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new AssignmentNotFoundException("Assignment not found"));

        accessPolicy.checkDeleteAccess(caller, assignment);
        assignmentRepository.delete(assignment);
        return "Assignment " + id + " successfully removed.";
    }

    @Override
    @Transactional
    public AssignmentResponse reassignBuruh(
            UUID assignmentId, ReassignmentRequest request, String adminId) {
        AuthUser admin = findUser(UUID.fromString(adminId), "Admin not found");
        requireRole(admin, Role.ADMIN, "Only ADMIN can reassign buruh");

        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new AssignmentNotFoundException("Assignment not found"));

        AuthUser newMandor = findUser(request.getNewMandorId(), "New mandor not found");
        requireRole(newMandor, Role.MANDOR, "New assignee must have the MANDOR role");

        if (assignment.getMandor().getId().equals(newMandor.getId())) {
            throw new RoleMismatchException(
                    "New mandor is the same as the current mandor");
        }

        UUID oldMandorId = assignment.getMandor().getId();
        UUID buruhId = assignment.getBuruh().getId();
        LocalDateTime reassignedAt = LocalDateTime.now();

        assignment.setMandor(newMandor);
        assignment.setReassignedAt(reassignedAt);
        Assignment saved = assignmentRepository.save(assignment);

        eventPublisher.publishEvent(new BuruhReassignedEvent(
                saved.getId(), buruhId, oldMandorId, newMandor.getId(), reassignedAt
        ));

        return mapper.toResponse(saved);
    }

    private AuthUser findUser(UUID id, String message) {
        return authUserRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(message));
    }

    private void requireRole(AuthUser user, Role required, String message) {
        if (!user.getRole().equals(required)) {
            throw new RoleMismatchException(message);
        }
    }
}
