package id.ac.ui.cs.advprog.mysawit.auth.service;

import id.ac.ui.cs.advprog.mysawit.auth.entity.Assignment;
import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser;

import java.util.UUID;

public interface AssignmentAccessPolicy {
    void checkReadAccess(AuthUser caller, Assignment assignment);
    void checkDeleteAccess(AuthUser caller, Assignment assignment);
    void checkBuruhQueryAccess(AuthUser caller, UUID buruhId);
    void checkMandorQueryAccess(AuthUser caller, UUID mandorId);
}
