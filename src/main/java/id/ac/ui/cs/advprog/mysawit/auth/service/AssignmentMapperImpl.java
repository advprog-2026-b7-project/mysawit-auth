package id.ac.ui.cs.advprog.mysawit.auth.service;

import id.ac.ui.cs.advprog.mysawit.auth.dto.AssignmentResponse;
import id.ac.ui.cs.advprog.mysawit.auth.entity.Assignment;
import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AssignmentMapperImpl implements AssignmentMapper {

    @Override
    public AssignmentResponse toResponse(Assignment assignment) {
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

    private String displayName(AuthUser user) {
        return user.getNama() != null ? user.getNama() : user.getUsername();
    }
}
