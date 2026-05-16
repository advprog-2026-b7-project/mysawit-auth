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
import id.ac.ui.cs.advprog.mysawit.auth.repository.AssignmentRepository;
import id.ac.ui.cs.advprog.mysawit.auth.repository.AuthUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssignmentServiceImpl implements AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final AuthUserRepository authUserRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public AssignmentResponse createAssignment(CreateAssignmentRequest request, String adminId) {
        AuthUser admin = findUserOrThrow(UUID.fromString(adminId), "Admin not found");
        requireRole(admin, Role.ADMIN, "Only ADMIN can create assignments");

        AuthUser buruh = findUserOrThrow(request.getBuruhId(), "Buruh not found");
        requireRole(buruh, Role.BURUH, "Target user must have the BURUH role");

        AuthUser mandor = findUserOrThrow(request.getMandorId(), "Mandor not found");
        requireRole(mandor, Role.MANDOR, "Assignee must have the MANDOR role");

        if (assignmentRepository.existsByBuruh(buruh)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Buruh already has an active assignment."
                    + " Use POST /api/assignments/{id}/reassign instead.");
        }

        Assignment saved = assignmentRepository.save(
                Assignment.builder().buruh(buruh).mandor(mandor).build()
        );

        eventPublisher.publishEvent(new BuruhAssignedEvent(
                saved.getId(), buruh.getId(), mandor.getId(), saved.getCreatedAt()
        ));

        return toResponse(saved);
    }

    @Override
    public Page<AssignmentResponse> getAllAssignments(AuthUser caller, Pageable pageable) {
        return switch (caller.getRole()) {
            case ADMIN -> assignmentRepository.findAll(pageable).map(this::toResponse);
            case MANDOR -> {
                AuthUser mandor = findUserOrThrow(caller.getId(), "Mandor not found");
                yield assignmentRepository.findByMandor(mandor, pageable).map(this::toResponse);
            }
            case BURUH -> {
                AuthUser buruh = findUserOrThrow(caller.getId(), "Buruh not found");
                yield assignmentRepository.findByBuruh(buruh, pageable).map(this::toResponse);
            }
            default -> assignmentRepository.findAll(pageable).map(this::toResponse);
        };
    }

    @Override
    public AssignmentResponse getAssignmentById(UUID id, AuthUser caller) {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Assignment not found"));

        validateReadAccess(caller, assignment);
        return toResponse(assignment);
    }

    @Override
    public AssignmentResponse getAssignmentByBuruhId(UUID buruhId, AuthUser caller) {
        if (caller.getRole() == Role.BURUH && !caller.getId().equals(buruhId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Buruh may only query their own assignment");
        }

        AuthUser buruh = findUserOrThrow(buruhId, "Buruh not found");
        Assignment assignment = assignmentRepository.findFirstByBuruh(buruh)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "This Buruh currently has no active assignment"));

        return toResponse(assignment);
    }

    @Override
    public MandorAssignmentsResponse getAssignmentsByMandorId(
            UUID mandorId, AuthUser caller, Pageable pageable) {
        if (caller.getRole() == Role.MANDOR && !caller.getId().equals(mandorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Mandor may only query their own assignments");
        }

        AuthUser mandor = findUserOrThrow(mandorId, "Mandor not found");
        requireRole(mandor, Role.MANDOR, "Specified user does not have the MANDOR role");

        Page<Assignment> page = assignmentRepository.findByMandor(mandor, pageable);
        List<AssignmentResponse> content =
                page.getContent().stream().map(this::toResponse).toList();

        return MandorAssignmentsResponse.builder()
                .mandorId(mandor.getId())
                .mandorNama(displayName(mandor))
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
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Assignment not found"));

        validateDeleteAccess(caller, assignment);
        assignmentRepository.delete(assignment);
        return "Assignment " + id + " successfully removed.";
    }

    @Override
    @Transactional
    public AssignmentResponse reassignBuruh(
            UUID assignmentId, ReassignmentRequest request, String adminId) {
        AuthUser admin = findUserOrThrow(UUID.fromString(adminId), "Admin not found");
        requireRole(admin, Role.ADMIN, "Only ADMIN can reassign buruh");

        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Assignment not found"));

        AuthUser newMandor = findUserOrThrow(request.getNewMandorId(), "New mandor not found");
        requireRole(newMandor, Role.MANDOR, "New assignee must have the MANDOR role");

        if (assignment.getMandor().getId().equals(newMandor.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "New mandor is the same as the current mandor");
        }

        UUID oldMandorId = assignment.getMandor().getId();
        UUID buruhId     = assignment.getBuruh().getId();
        LocalDateTime reassignedAt = LocalDateTime.now();

        assignment.setMandor(newMandor);
        assignment.setReassignedAt(reassignedAt);
        Assignment saved = assignmentRepository.save(assignment);

        eventPublisher.publishEvent(new BuruhReassignedEvent(
                saved.getId(), buruhId, oldMandorId, newMandor.getId(), reassignedAt
        ));

        return toResponse(saved);
    }

    private AuthUser findUserOrThrow(UUID id, String message) {
        return authUserRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, message));
    }

    private void requireRole(AuthUser user, Role required, String message) {
        if (!user.getRole().equals(required)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private void validateReadAccess(AuthUser caller, Assignment assignment) {
        if (caller.getRole() == Role.BURUH
                && !caller.getId().equals(assignment.getBuruh().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied to this assignment");
        }
        if (caller.getRole() == Role.MANDOR
                && !caller.getId().equals(assignment.getMandor().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied to this assignment");
        }
    }

    private void validateDeleteAccess(AuthUser caller, Assignment assignment) {
        if (caller.getRole() == Role.BURUH
                && !caller.getId().equals(assignment.getBuruh().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Buruh may only remove their own assignment");
        }
        if (caller.getRole() == Role.MANDOR
                && !caller.getId().equals(assignment.getMandor().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Mandor may only remove assignments of their own Buruh");
        }
    }

    private String displayName(AuthUser user) {
        return user.getNama() != null ? user.getNama() : user.getUsername();
    }

    private AssignmentResponse toResponse(Assignment assignment) {
        return AssignmentResponse.builder()
                .id(assignment.getId())
                .buruhId(assignment.getBuruh().getId())
                .buruhNama(displayName(assignment.getBuruh()))
                .mandorId(assignment.getMandor().getId())
                .mandorNama(displayName(assignment.getMandor()))
                .assignedAt(assignment.getCreatedAt())
                .reassignedAt(assignment.getReassignedAt())
                .build();
    }
}
