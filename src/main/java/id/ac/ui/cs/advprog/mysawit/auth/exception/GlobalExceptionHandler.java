package id.ac.ui.cs.advprog.mysawit.auth.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "status", "error",
                "message", "Access denied",
                "errors", List.of()
        ));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "status", "error",
                "message", ex.getMessage() != null ? ex.getMessage() : "Unauthorized",
                "errors", List.of()
        ));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of(
                "status", "error",
                "message", ex.getReason() != null ? ex.getReason() : ex.getMessage(),
                "errors", List.of()
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "status", "error",
                "message", "Validation failed",
                "errors", errors
        ));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "status", "error",
                "message", ex.getMessage(),
                "errors", List.of()
        ));
    }

    @ExceptionHandler(AssignmentNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleAssignmentNotFound(
            AssignmentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "status", "error",
                "message", ex.getMessage(),
                "errors", List.of()
        ));
    }

    @ExceptionHandler(RoleMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleRoleMismatch(RoleMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "status", "error",
                "message", ex.getMessage(),
                "errors", List.of()
        ));
    }

    @ExceptionHandler(DuplicateAssignmentException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateAssignment(
            DuplicateAssignmentException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "status", "error",
                "message", ex.getMessage(),
                "errors", List.of()
        ));
    }

    @ExceptionHandler(AssignmentForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleAssignmentForbidden(
            AssignmentForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "status", "error",
                "message", ex.getMessage(),
                "errors", List.of()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "An unexpected error occurred",
                "errors", List.of()
        ));
    }
}
