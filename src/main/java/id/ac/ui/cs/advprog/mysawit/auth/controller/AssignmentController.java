package id.ac.ui.cs.advprog.mysawit.auth.controller;

import id.ac.ui.cs.advprog.mysawit.auth.dto.*;
import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser;
import id.ac.ui.cs.advprog.mysawit.auth.service.AssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AssignmentController {

    private final AssignmentService assignmentService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AssignmentResponse>> createAssignment(
            @RequestBody CreateAssignmentRequest request,
            Authentication authentication) {
        AuthUser admin = (AuthUser) authentication.getPrincipal();
        AssignmentResponse data =
                assignmentService.createAssignment(request, admin.getId().toString());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(data));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AssignmentResponse>>> getAllAssignments(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        AuthUser caller = (AuthUser) authentication.getPrincipal();
        Page<AssignmentResponse> page = assignmentService.getAllAssignments(caller, pageable);

        PageResponse<AssignmentResponse> pageResponse = PageResponse.<AssignmentResponse>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();

        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AssignmentResponse>> getAssignmentById(
            @PathVariable UUID id,
            Authentication authentication) {
        AuthUser caller = (AuthUser) authentication.getPrincipal();
        AssignmentResponse data = assignmentService.getAssignmentById(id, caller);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/buruh/{buruhId}")
    public ResponseEntity<ApiResponse<AssignmentResponse>> getAssignmentByBuruhId(
            @PathVariable UUID buruhId,
            Authentication authentication) {
        AuthUser caller = (AuthUser) authentication.getPrincipal();
        AssignmentResponse data = assignmentService.getAssignmentByBuruhId(buruhId, caller);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/mandor/{mandorId}")
    public ResponseEntity<ApiResponse<MandorAssignmentsResponse>> getAssignmentsByMandorId(
            @PathVariable UUID mandorId,
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        AuthUser caller = (AuthUser) authentication.getPrincipal();
        MandorAssignmentsResponse data =
                assignmentService.getAssignmentsByMandorId(mandorId, caller, pageable);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAssignment(
            @PathVariable UUID id,
            @RequestBody(required = false) DeleteAssignmentRequest request,
            Authentication authentication) {
        AuthUser caller = (AuthUser) authentication.getPrincipal();
        if (request != null && request.getNewMandorId() != null) {
            AssignmentResponse data =
                    assignmentService.reassignOnDelete(id, caller, request.getNewMandorId());
            return ResponseEntity.ok(ApiResponse.success(data));
        }
        String message = assignmentService.deleteAssignment(id, caller);
        return ResponseEntity.ok(Map.of("message", message));
    }

    @PostMapping("/{id}/reassign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AssignmentResponse>> reassignBuruh(
            @PathVariable UUID id,
            @RequestBody ReassignmentRequest request,
            Authentication authentication) {
        AuthUser admin = (AuthUser) authentication.getPrincipal();
        AssignmentResponse data =
                assignmentService.reassignBuruh(id, request, admin.getId().toString());
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
