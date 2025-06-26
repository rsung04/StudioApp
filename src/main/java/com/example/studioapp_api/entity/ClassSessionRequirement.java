package com.example.studioapp_api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "class_session_requirements", uniqueConstraints = {
    @UniqueConstraint(name = "uq_class_session_req", 
                      columnNames = {"term_id", "class_definition_id", "studio_location_id"})
    // Note: Handling uniqueness for NULL studio_location_id in JPA is tricky.
    // The database constraint `DEFERRABLE INITIALLY DEFERRED` along with potential partial indexes
    // is more robust for conditional uniqueness with NULLs.
    // We will rely on the DB for full enforcement.
    // A service-level check can be added.
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassSessionRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "sessions_per_week", nullable = false)
    private Integer sessionsPerWeek; // DB CHECK (sessions_per_week > 0)

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // --- Relationships ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id", nullable = false)
    private Term term;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_definition_id", nullable = false)
    private ClassDefinition classDefinition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "studio_location_id") // Nullable
    private StudioLocation studioLocation; // Can be null

    @PrePersist
    @PreUpdate
    private void validateRequirement() {
        if (sessionsPerWeek == null || sessionsPerWeek <= 0) {
            throw new IllegalArgumentException("Sessions per week must be positive.");
        }
    }
}