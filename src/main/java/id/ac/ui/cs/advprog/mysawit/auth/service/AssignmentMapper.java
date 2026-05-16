package id.ac.ui.cs.advprog.mysawit.auth.service;

import id.ac.ui.cs.advprog.mysawit.auth.dto.AssignmentResponse;
import id.ac.ui.cs.advprog.mysawit.auth.entity.Assignment;

public interface AssignmentMapper {
    AssignmentResponse toResponse(Assignment assignment);
}
