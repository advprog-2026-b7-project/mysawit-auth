package id.ac.ui.cs.advprog.mysawit.auth.dto;

import id.ac.ui.cs.advprog.mysawit.auth.entity.Role;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class AdminUserDetailResponse {
    UUID id;
    String username;
    String nama;
    String email;
    Role role;
    BigDecimal walletBalance;
    String mandorCertificationNumber;
    UUID mandorId;
    UUID kebunId;
    String kebunNama;
    LocalDateTime createdAt;
}
