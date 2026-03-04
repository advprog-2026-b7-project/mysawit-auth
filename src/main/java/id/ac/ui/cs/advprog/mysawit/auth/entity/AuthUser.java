package id.ac.ui.cs.advprog.mysawit.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;
@Entity
@Table(name = "auth_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String username;
}