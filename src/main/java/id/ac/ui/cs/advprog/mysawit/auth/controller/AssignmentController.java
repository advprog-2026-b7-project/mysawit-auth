package id.ac.ui.cs.advprog.mysawit.auth.controller;

import id.ac.ui.cs.advprog.mysawit.auth.dto.AssignmentResponse;
import id.ac.ui.cs.advprog.mysawit.auth.dto.CreateAssignmentRequest;
import id.ac.ui.cs.advprog.mysawit.auth.dto.ReassignmentRequest;
import id.ac.ui.cs.advprog.mysawit.auth.dto.ReassignmentResponse;
import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser;
import id.ac.ui.cs.advprog.mysawit.auth.service.AssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    @PostMapping
    public ResponseEntity<AssignmentResponse> createAssignment(
            @RequestBody CreateAssignmentRequest request,
            Authentication authentication) {
        try {
            AuthUser admin = (AuthUser) authentication.getPrincipal();
            String adminId = admin.getId().toString();
            AssignmentResponse response = assignmentService.createAssignment(request, adminId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssignmentResponse> getAssignmentById(@PathVariable UUID id) {
        try {
            AssignmentResponse response = assignmentService.getAssignmentById(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping
    public ResponseEntity<List<AssignmentResponse>> getAllAssignments() {
        List<AssignmentResponse> assignments = assignmentService.getAllAssignments();
        return ResponseEntity.ok(assignments);
    }

    @GetMapping("/buruh/{buruhId}")
    public ResponseEntity<List<AssignmentResponse>> getAssignmentsByBuruhId(@PathVariable UUID buruhId) {
        try {
            List<AssignmentResponse> assignments = assignmentService.getAssignmentsByBuruhId(buruhId);
            return ResponseEntity.ok(assignments);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/mandor/{mandorId}")
    public ResponseEntity<List<AssignmentResponse>> getAssignmentsByMandorId(@PathVariable UUID mandorId) {
        try {
            List<AssignmentResponse> assignments = assignmentService.getAssignmentsByMandorId(mandorId);
            return ResponseEntity.ok(assignments);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAssignment(@PathVariable UUID id) {
        try {
            assignmentService.deleteAssignment(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping("/{id}/reassign")
    public ResponseEntity<ReassignmentResponse> reassignBuruh(
            @PathVariable UUID id,
            @RequestBody ReassignmentRequest request,
            Authentication authentication) {
        try {
            AuthUser admin = (AuthUser) authentication.getPrincipal();
            String adminId = admin.getId().toString();
            ReassignmentResponse response = assignmentService.reassignBuruh(id, request, adminId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
