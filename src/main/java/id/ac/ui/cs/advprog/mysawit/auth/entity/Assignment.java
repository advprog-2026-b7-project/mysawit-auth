package id.ac.ui.cs.advprog.mysawit.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "buruh_id", nullable = false)
    private AuthUser buruh;

    @ManyToOne
    @JoinColumn(name = "mandor_id", nullable = false)
    private AuthUser mandor;

    @Column(name = "plantation_id")
    private String plantationId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "reassigned_at")
    private LocalDateTime reassignedAt;

    public LocalDateTime getReassignedAt() {
        return reassignedAt;
    }

    public void setReassignedAt(LocalDateTime reassignedAt) {
        this.reassignedAt = reassignedAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
