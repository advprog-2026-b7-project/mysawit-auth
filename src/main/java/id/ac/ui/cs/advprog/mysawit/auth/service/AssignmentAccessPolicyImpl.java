package id.ac.ui.cs.advprog.mysawit.auth.service;

import id.ac.ui.cs.advprog.mysawit.auth.entity.Assignment;
import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser;
import id.ac.ui.cs.advprog.mysawit.auth.entity.Role;
import id.ac.ui.cs.advprog.mysawit.auth.exception.AssignmentForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AssignmentAccessPolicyImpl implements AssignmentAccessPolicy {

    @Override
    public void checkReadAccess(AuthUser caller, Assignment assignment) {
        if (caller.getRole() == Role.BURUH
                && !caller.getId().equals(assignment.getBuruh().getId())) {
            throw new AssignmentForbiddenException("Access denied to this assignment");
        }
        if (caller.getRole() == Role.MANDOR
                && !caller.getId().equals(assignment.getMandor().getId())) {
            throw new AssignmentForbiddenException("Access denied to this assignment");
        }
    }

    @Override
    public void checkDeleteAccess(AuthUser caller, Assignment assignment) {
        if (caller.getRole() == Role.BURUH
                && !caller.getId().equals(assignment.getBuruh().getId())) {
            throw new AssignmentForbiddenException("Buruh may only remove their own assignment");
        }
        if (caller.getRole() == Role.MANDOR
                && !caller.getId().equals(assignment.getMandor().getId())) {
            throw new AssignmentForbiddenException(
                    "Mandor may only remove assignments of their own Buruh");
        }
    }

    @Override
    public void checkBuruhQueryAccess(AuthUser caller, UUID buruhId) {
        if (caller.getRole() == Role.BURUH && !caller.getId().equals(buruhId)) {
            throw new AssignmentForbiddenException(
                    "Buruh may only query their own assignment");
        }
    }

    @Override
    public void checkMandorQueryAccess(AuthUser caller, UUID mandorId) {
        if (caller.getRole() == Role.MANDOR && !caller.getId().equals(mandorId)) {
            throw new AssignmentForbiddenException(
                    "Mandor may only query their own assignments");
        }
    }
}
